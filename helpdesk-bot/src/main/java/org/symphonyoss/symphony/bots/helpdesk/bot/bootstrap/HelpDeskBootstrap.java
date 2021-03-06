package org.symphonyoss.symphony.bots.helpdesk.bot.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.exceptions.InitException;
import org.symphonyoss.client.model.SymAuth;
import org.symphonyoss.symphony.bots.ai.helpdesk.HelpDeskAi;
import org.symphonyoss.symphony.bots.ai.helpdesk.conversation.IdleTimerManager;
import org.symphonyoss.symphony.bots.ai.helpdesk.conversation.ProxyIdleTimer;
import org.symphonyoss.symphony.bots.helpdesk.bot.HelpDeskBot;
import org.symphonyoss.symphony.bots.helpdesk.bot.authentication.HelpDeskAuthenticationException;
import org.symphonyoss.symphony.bots.helpdesk.bot.authentication.HelpDeskAuthenticationService;
import org.symphonyoss.symphony.bots.helpdesk.bot.client.HelpDeskHttpClient;
import org.symphonyoss.symphony.bots.helpdesk.bot.client.HelpDeskSymphonyClient;
import org.symphonyoss.symphony.bots.helpdesk.bot.config.HelpDeskBotConfig;
import org.symphonyoss.symphony.bots.helpdesk.bot.listener.AutoConnectionAcceptListener;
import org.symphonyoss.symphony.bots.helpdesk.bot.listener.HelpDeskRoomEventListener;
import org.symphonyoss.symphony.bots.helpdesk.bot.provisioning.HelpDeskProvisioningService;
import org.symphonyoss.symphony.bots.helpdesk.messageproxy.ChatListener;
import org.symphonyoss.symphony.bots.helpdesk.messageproxy.IdleMessageService;
import org.symphonyoss.symphony.bots.helpdesk.messageproxy.config.IdleTicketConfig;
import org.symphonyoss.symphony.bots.helpdesk.service.model.Membership;
import org.symphonyoss.symphony.bots.helpdesk.service.ticket.client.TicketClient;
import org.symphonyoss.symphony.bots.utility.function.FunctionExecutor;

import java.util.concurrent.TimeUnit;

/**
 * Listener to indicate the application is ready to service requests.
 *
 * Created by rsanchez on 15/12/17.
 */
public class HelpDeskBootstrap implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelpDeskBootstrap.class);

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    ApplicationContext applicationContext = event.getApplicationContext();
    execute(applicationContext);
  }

  /**
   * The bootstrap process include the following steps:
   * - Create shared HTTP client
   * - Authenticate bot user
   * - Create Symphony client
   * - Register bot as member of the group (if required)
   * - Initialize AI
   * @param applicationContext Spring Application context
   */
  public void execute(ApplicationContext applicationContext) {
    FunctionExecutor<ApplicationContext, HelpDeskProvisioningService> functionProvisioning =
        new FunctionExecutor<>();
    functionProvisioning
        .function(context -> executeProvisioning(context))
        .onError(e -> LOGGER.error("Fail to execute provisioning process", e));

    FunctionExecutor<ApplicationContext, HelpDeskHttpClient> functionHttpClient = new FunctionExecutor<>();
    functionHttpClient
        .function(context -> setupHttpClient(context))
        .onError(e -> LOGGER.error("Fail to create HTTP client", e));

    FunctionExecutor<ApplicationContext, SymAuth> functionAuth = new FunctionExecutor<>();
    functionAuth
        .function(context -> authenticateUser(context))
        .onError(e -> LOGGER.error("Fail to authenticate user", e));

    FunctionExecutor<SymAuth, SymphonyClient> functionClient = new FunctionExecutor<>();
    functionClient
        .function(auth -> initSymphonyClient(applicationContext, auth))
        .onError(e -> LOGGER.error("Fail to create symphony client", e));

    FunctionExecutor<ApplicationContext, Membership> functionRegisterBot = new FunctionExecutor<>();
    functionRegisterBot
        .function(context -> registerBot(context))
        .onError(e -> LOGGER.error("Fail to register bot user", e));

    FunctionExecutor<SymAuth, IdleTimerManager> functionIdleTimeManager = new FunctionExecutor<>();
    functionIdleTimeManager
        .function(auth -> initializeIdleTimeManager(applicationContext, auth))
        .onError(e -> LOGGER.error("Fail to initialize Idle Timer Manager", e));

    try {
      functionProvisioning.executeBackoffExponential(applicationContext);
      functionHttpClient.executeBackoffExponential(applicationContext);
      SymAuth symAuth = functionAuth.executeBackoffExponential(applicationContext);
      functionClient.executeBackoffExponential(symAuth);
      functionRegisterBot.executeBackoffExponential(applicationContext);
      functionIdleTimeManager.executeBackoffExponential(symAuth);

      ready(applicationContext);
    } catch (InterruptedException e) {
      LOGGER.error("Fail to start helpdesk bot", e);
    }
  }

  /**
   * Initialize a time manager to get all unserviced ticket and set idle timer on them.
   * @param context Spring application context
   * @param symAuth Symphony authentication
   * @return IdleTimeManager
   */

  private IdleTimerManager initializeIdleTimeManager(ApplicationContext context, SymAuth symAuth) {
    IdleTimerManager timerManager = context.getBean(IdleTimerManager.class);
    TicketClient ticketClient = context.getBean(TicketClient.class);
    IdleTicketConfig idleTicketConfig = context.getBean(IdleTicketConfig.class);
    IdleMessageService idleMessageService = context.getBean(IdleMessageService.class);

    Long timeout = idleTicketConfig.getTimeout();
    TimeUnit timeUnit = idleTicketConfig.getUnit();

    String jwt = symAuth.getSessionToken().getToken();

    ticketClient.getUnresolvedTickets(jwt).stream()
        .forEach((ticket)-> timerManager.put(ticket.getId(),
        new ProxyIdleTimer(timeout, timeUnit ) {
          @Override
          public void onIdleTimeout() {
            idleMessageService.sendIdleMessage(ticket);
          }
        }));

    return timerManager;
  }

  /**
   * Create HTTP client.
   * @param applicationContext Spring application context
   * @return Symphony HTTP client
   */
  private HelpDeskHttpClient setupHttpClient(ApplicationContext applicationContext) {
    HelpDeskHttpClient httpClient = applicationContext.getBean(HelpDeskHttpClient.class);
    HelpDeskBotConfig config = applicationContext.getBean(HelpDeskBotConfig.class);

    httpClient.setupClient(config);

    return httpClient;
  }

  /**
   * Authenticates bot user
   * @param applicationContext Spring application context
   * @return Symphony authentication
   */
  private SymAuth authenticateUser(ApplicationContext applicationContext) {
    HelpDeskAuthenticationService authService = applicationContext.getBean(HelpDeskAuthenticationService.class);
    return authService.authenticate();
  }

  /**
   * Authenticates bot user, starts symphony client, register listeners and accept incoming requests
   * from external users.
   * @param applicationContext Spring application context
   */
  private HelpDeskSymphonyClient initSymphonyClient(ApplicationContext applicationContext,
      SymAuth symAuth) {
    try {
      HelpDeskSymphonyClient symphonyClient = applicationContext.getBean(HelpDeskSymphonyClient.class);
      HelpDeskBotConfig config = applicationContext.getBean(HelpDeskBotConfig.class);

      symphonyClient.init(symAuth, config.getEmail(), config.getAgentUrl(), config.getPodUrl());

      registerListeners(symphonyClient, applicationContext);

      acceptIncomingRequests(applicationContext);

      return symphonyClient;
    } catch (HelpDeskAuthenticationException | InitException e) {
      throw new HelpDeskBootstrapException("Fail to start helpdesk bot", e);
    }
  }

  /**
   * Register listeners
   * @param symphonyClient Symphony client
   * @param applicationContext Spring application context
   */
  private void registerListeners(HelpDeskSymphonyClient symphonyClient,
      ApplicationContext applicationContext) {
    ChatListener chatListener = applicationContext.getBean(ChatListener.class);
    HelpDeskRoomEventListener roomEventListener = applicationContext.getBean(HelpDeskRoomEventListener.class);
    AutoConnectionAcceptListener connectionListener = applicationContext.getBean(AutoConnectionAcceptListener.class);

    symphonyClient.getMessageService().addRoomServiceEventListener(roomEventListener);
    symphonyClient.getMessageService().addConnectionsEventListener(connectionListener);
    symphonyClient.getMessageService().addMessageListener(chatListener);
  }

  /**
   * Accept incoming requests from the external users
   * @param applicationContext Spring application context
   */
  private void acceptIncomingRequests(ApplicationContext applicationContext) {
    AutoConnectionAcceptListener connectionListener = applicationContext.getBean(AutoConnectionAcceptListener.class);
    connectionListener.acceptAllIncomingRequests();
  }

  /**
   * Register bot as a group id member
   * @param applicationContext Spring application context
   */
  private Membership registerBot(ApplicationContext applicationContext) {
    HelpDeskBot helpDeskBot = applicationContext.getBean(HelpDeskBot.class);
    return helpDeskBot.registerDefaultAgent();
  }

  /**
   * Indicates the application is ready to service requests.
   * @param applicationContext Spring application context
   */
  private void ready(ApplicationContext applicationContext) {
    HelpDeskBot helpDeskBot = applicationContext.getBean(HelpDeskBot.class);
    HelpDeskBotConfig configuration = applicationContext.getBean(HelpDeskBotConfig.class);

    helpDeskBot.ready();

    LOGGER.info("Help Desk Bot startup complete for groupId: " + configuration.getGroupId());
  }

  /**
   * Executes the provisioning process.
   * @param applicationContext Spring application context
   */
  private HelpDeskProvisioningService executeProvisioning(ApplicationContext applicationContext) {
    HelpDeskProvisioningService provisioningService = applicationContext.getBean(
        HelpDeskProvisioningService.class);
    provisioningService.execute();

    return provisioningService;
  }

}
