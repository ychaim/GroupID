package org.symphonyoss.symphony.bots.ai.command;

import org.symphonyoss.symphony.bots.ai.AiAction;
import org.symphonyoss.symphony.bots.ai.AiResponder;
import org.symphonyoss.symphony.bots.ai.AiResponseIdentifier;
import org.symphonyoss.symphony.bots.ai.HelpDeskAiSession;
import org.symphonyoss.symphony.bots.ai.common.HelpDeskAiConstants;
import org.symphonyoss.symphony.bots.ai.config.HelpDeskAiConfig;
import org.symphonyoss.symphony.bots.ai.impl.AiResponseIdentifierImpl;
import org.symphonyoss.symphony.bots.ai.model.AiArgumentMap;
import org.symphonyoss.symphony.bots.ai.model.AiCommand;
import org.symphonyoss.symphony.bots.ai.model.AiMessage;
import org.symphonyoss.symphony.bots.ai.model.AiResponse;
import org.symphonyoss.symphony.bots.ai.model.AiSessionContext;
import org.symphonyoss.symphony.bots.ai.model.ArgumentType;
import org.symphonyoss.symphony.bots.ai.HelpDeskAiSessionContext;
import org.symphonyoss.symphony.bots.ai.HelpDeskAiSessionKey;
import org.symphonyoss.symphony.bots.helpdesk.service.client.TicketClient;
import org.symphonyoss.symphony.bots.helpdesk.service.model.Ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.exceptions.SymException;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by nick.tarsillo on 10/10/17.
 */
public class AcceptTicketCommand extends AiCommand {
  private static final Logger LOG = LoggerFactory.getLogger(AcceptTicketCommand.class);

  public AcceptTicketCommand(String command, String usage) {
    super(command, usage);
    setArgumentTypes(ArgumentType.STRING);
    addAction(new AcceptAction());
  }

  class AcceptAction implements AiAction {
    @Override
    public void doAction(AiSessionContext sessionContext, AiResponder responder,
        AiArgumentMap aiArgumentMap) {
      HelpDeskAiSessionKey aiSessionKey = (HelpDeskAiSessionKey) sessionContext.getAiSessionKey();
      HelpDeskAiSessionContext aiSessionContext = (HelpDeskAiSessionContext) sessionContext;
      HelpDeskAiSession helpDeskAiSession = aiSessionContext.getHelpDeskAiSession();
      HelpDeskAiConfig helpDeskAiConfig = helpDeskAiSession.getHelpDeskAiConfig();

      Set<String> keySet = aiArgumentMap.getKeySet();
      if (keySet.size() != 0) {
        Ticket ticket = helpDeskAiSession.getTicketClient()
            .getTicket(aiArgumentMap.getArgumentAsString(keySet.iterator().next()));
        if(ticket != null) {
          try {
            helpDeskAiSession.getSymphonyClient().getRoomMembershipClient().addMemberToRoom(
                ticket.getServiceStreamId(), Long.parseLong(aiSessionKey.getUid()));
            ticket.setState(TicketClient.TicketStateType.UNRESOLVED.getState());
            helpDeskAiSession.getTicketClient().updateTicket(ticket);
            responder.addResponse(sessionContext, successResponse(helpDeskAiConfig, aiSessionKey));
            responder.addResponse(sessionContext, successResponseClient(helpDeskAiConfig, ticket));
          } catch (SymException e) {
            LOG.error("Failed to add agent to service room: ", e);
            responder.addResponse(sessionContext, failedToAddAgentToService(aiSessionKey));
          }
        } else {
          responder.addResponse(sessionContext, ticketNotFoundResponse(aiSessionKey));
        }
      } else {
        responder.respondWithUseMenu(sessionContext);
      }

      responder.respond(sessionContext);
    }

    private AiResponse successResponse(HelpDeskAiConfig helpDeskAiConfig, HelpDeskAiSessionKey aiSessionKey) {
      return response(helpDeskAiConfig.getAcceptTicketAgentSuccessResponse(), aiSessionKey.getStreamId());
    }

    private AiResponse successResponseClient(HelpDeskAiConfig helpDeskAiConfig, Ticket ticket) {
      return response(helpDeskAiConfig.getAcceptTicketClientSuccessResponse(), ticket.getClientStreamId());
    }

    private AiResponse ticketNotFoundResponse(HelpDeskAiSessionKey aiSessionKey) {
      return response(HelpDeskAiConstants.TICKET_NOT_FOUND, aiSessionKey.getStreamId());
    }

    private AiResponse failedToAddAgentToService(HelpDeskAiSessionKey aiSessionKey) {
      return response(HelpDeskAiConstants.INTERNAL_ERROR, aiSessionKey.getStreamId());
    }

    private AiResponse response(String message, String stream) {
      AiMessage aiMessage = new AiMessage(message);
      Set<AiResponseIdentifier> responseIdentifiers = new HashSet<>();
      responseIdentifiers.add(new AiResponseIdentifierImpl(stream));
      return new AiResponse(aiMessage, responseIdentifiers);
    }
  }
}