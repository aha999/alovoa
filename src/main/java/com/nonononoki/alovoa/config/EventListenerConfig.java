package com.nonononoki.alovoa.config;

import java.util.List;

import com.nonononoki.alovoa.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDeleteParams;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.repo.UserLikeRepository;
import com.nonononoki.alovoa.repo.UserNotificationRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.UserService;

@Component
public class EventListenerConfig {

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private UserLikeRepository userLikeRepo;

	@Autowired
	private UserHideRepository userHideRepo;

	@Autowired
	private UserBlockRepository userBlockRepo;

	@Autowired
	private UserReportRepository userReportRepo;

	@Autowired
	private UserNotificationRepository userNotificationRepo;

	@Autowired
	private ConversationRepository conversationRepo;
	@Autowired
	private GenderRepository genderRepo;

	@Autowired
	private UserIntentionRepository userIntentionRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${app.admin.email}")
	private String adminEmail;

	@Value("${app.admin.key}")
	private String adminKey;

	private static final Logger logger = LoggerFactory.getLogger(EventListenerConfig.class);

	@EventListener
	public void handleContextRefresh(ApplicationStartedEvent event) throws AlovoaException {
		setDefaultAdmin();
		setDefaultGenders();
		setDefaulIntentions();
		updateAllUsersAgePreferences();
	}

	public void setDefaultAdmin() {
		long noUsers = userRepo.count();
		if (noUsers == 0) {
			User user = new User(adminEmail);
			user.setAdmin(true);
			String enc = passwordEncoder.encode(adminKey);
			user.setPassword(enc);
			userRepo.save(user);
		}
	}

	public void setDefaultGenders() {

		long noGenders = genderRepo.count();

		if (noGenders == 0) {
			Gender male = new Gender();
			male.setText("male");
			genderRepo.saveAndFlush(male);

			Gender female = new Gender();
			female.setText("female");
			genderRepo.saveAndFlush(female);

			Gender other = new Gender();
			other.setText("other");
			genderRepo.saveAndFlush(other);
		}

	}

	public void setDefaulIntentions() {
		long noIntentions = userIntentionRepo.count();
		if (noIntentions == 0) {
			UserIntention meet = new UserIntention();
			meet.setText("meet");
			userIntentionRepo.saveAndFlush(meet);

			UserIntention date = new UserIntention();
			date.setText("date");
			userIntentionRepo.saveAndFlush(date);

			UserIntention sex = new UserIntention();
			sex.setText("sex");
			userIntentionRepo.saveAndFlush(sex);
		}
	}

	public void removeInvalidUsers() throws AlovoaException {
		List<User> users = userRepo.findAll();

		UserDeleteParams userDeleteParam = UserDeleteParams.builder().conversationRepo(conversationRepo)
				.userBlockRepo(userBlockRepo).userHideRepo(userHideRepo).userLikeRepo(userLikeRepo)
				.userNotificationRepo(userNotificationRepo).userRepo(userRepo).userReportRepo(userReportRepo).build();

		for (User user : users) {

			if (!user.getEmail().contains("@")) {

				try {
					UserService.removeUserDataCascading(user, userDeleteParam);
					userRepo.delete(userRepo.findByEmail(user.getEmail()));
					userRepo.flush();

				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	public void updateAllUsersAgePreferences() throws AlovoaException {
		List<User> users = userRepo.findAll();

		for (User user : users) {
			try {
				user.setPreferedMinAge(Tools.convertPrefAgeToRelativeYear(user.getDates().getDateOfBirth(), user.getPreferedMinAge()));
				user.setPreferedMaxAge(Tools.convertPrefAgeToRelativeYear(user.getDates().getDateOfBirth(), user.getPreferedMaxAge()));
				userRepo.saveAndFlush(user);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
	}
}
