package com.kauan.gamelog.user;

import com.kauan.gamelog.shared.ConflictException;
import com.kauan.gamelog.shared.JsonMergePatch;
import com.kauan.gamelog.shared.NotFoundException;
import com.kauan.gamelog.shared.UnprocessableException;
import com.kauan.gamelog.user.dto.MeDTO;
import com.kauan.gamelog.user.dto.ProfileForm;
import com.kauan.gamelog.user.dto.UpdateSettingsRequest;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final JsonMergePatch mergePatch;

    /** Comparado quando o login não existe, para a resposta demorar o mesmo e não revelar quem tem conta. */
    private final String unknownUserHash;

    public UserService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events,
            JsonMergePatch mergePatch) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.mergePatch = mergePatch;
        this.unknownUserHash = passwordEncoder.encode("conta-que-nao-existe");
    }

    @Transactional
    public AuthUser register(String username, String email, String password) {
        String name = normalize(username);
        String mail = normalize(email);
        if (users.existsByUsername(name)) {
            throw usernameTaken();
        }
        if (users.existsByEmail(mail)) {
            throw new ConflictException("Esse e-mail já tem uma conta.");
        }
        return AuthUser.of(save(new User(name, mail, passwordEncoder.encode(password))));
    }

    /** Login por username ou e-mail (RF02). */
    @Transactional(readOnly = true)
    public Optional<AuthUser> authenticate(String login, String password) {
        String key = normalize(login);
        Optional<User> user = key.contains("@") ? users.findByEmail(key) : users.findByUsername(key);
        boolean matches = passwordEncoder.matches(
                password, user.map(User::getPasswordHash).orElse(unknownUserHash));
        return user.filter(found -> matches).map(AuthUser::of);
    }

    @Transactional(readOnly = true)
    public Optional<AuthUser> findAuthUser(long id) {
        return users.findById(id).map(AuthUser::of);
    }

    @Transactional(readOnly = true)
    public Optional<PublicUser> findPublic(String username) {
        return users.findByUsername(normalize(username)).map(PublicUser::of);
    }

    @Transactional(readOnly = true)
    public MeDTO getMe(long id) {
        return MeDTO.from(find(id));
    }

    /** JSON Merge Patch sobre {@link ProfileForm}: o que não veio fica igual, e {@code null} limpa. */
    @Transactional
    public MeDTO updateProfile(long id, JsonNode patch) {
        User user = find(id);
        ProfileForm form = mergePatch.apply(ProfileForm.of(user), patch);
        String name = normalize(form.username());
        if (!name.equals(user.getUsername()) && users.existsByUsername(name)) {
            throw usernameTaken();
        }
        user.setUsername(name);
        user.setDisplayName(blankToNull(form.displayName()));
        user.setBio(blankToNull(form.bio()));
        user.setGender(form.gender());
        return MeDTO.from(save(user));
    }

    @Transactional
    public MeDTO updateSettings(long id, UpdateSettingsRequest changes) {
        User user = find(id);
        if (changes.profileVisibility() != null) {
            user.setProfileVisibility(changes.profileVisibility());
        }
        if (changes.showSpending() != null) {
            user.setShowSpending(changes.showSpending());
        }
        if (changes.defaultCurrency() != null) {
            user.setDefaultCurrency(currency(changes.defaultCurrency()));
        }
        return MeDTO.from(user);
    }

    /** Também encerra todas as sessões abertas (evento {@link PasswordChanged}). */
    @Transactional
    public void changePassword(long id, String currentPassword, String newPassword) {
        User user = find(id);
        checkPassword(user, currentPassword, "currentPassword");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        events.publishEvent(new PasswordChanged(id));
    }

    /** RN12: o banco apaga junto as sessões e, na 1.4, a biblioteca e as avaliações. */
    @Transactional
    public void delete(long id, String password) {
        User user = find(id);
        checkPassword(user, password, "password");
        users.delete(user);
    }

    private User find(long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Conta não encontrada."));
    }

    private User save(User user) {
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Esse username ou e-mail acabou de ser usado por outra conta.");
        }
    }

    private void checkPassword(User user, String password, String field) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw UnprocessableException.field("WRONG_PASSWORD", field, "senha incorreta");
        }
    }

    private static String currency(String code) {
        try {
            return Currency.getInstance(code).getCurrencyCode();
        } catch (IllegalArgumentException e) {
            throw UnprocessableException.field("UNKNOWN_CURRENCY", "defaultCurrency", "moeda desconhecida: " + code);
        }
    }

    private static ConflictException usernameTaken() {
        return new ConflictException("Esse username já está em uso.");
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
