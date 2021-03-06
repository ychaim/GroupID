package org.symphonyoss.symphony.bots.ai.impl;

import org.symphonyoss.symphony.bots.ai.AiResponder;
import org.symphonyoss.symphony.bots.ai.common.AiConstants;
import org.symphonyoss.symphony.bots.ai.model.AiCommandMenu;
import org.symphonyoss.symphony.bots.ai.model.AiMessage;
import org.symphonyoss.symphony.bots.ai.model.AiResponse;
import org.symphonyoss.symphony.bots.ai.model.AiSessionKey;

/**
 * Concrete implementation of {@link AiResponder}.
 * <p>
 * Created by nick.tarsillo on 8/21/17.
 */
public abstract class SymphonyAiResponder implements AiResponder {

  @Override
  public void respond(AiResponse... responses) {
    for (AiResponse aiResponse : responses) {
      for (String streamId : aiResponse.getRespondTo()) {
        publishMessage(streamId, aiResponse.getMessage());
      }
    }
  }

  /**
   * Send the message over Symphony's message client using MessageML.
   * @param streamId Stream identifier to where the message should be sent
   * @param aiMessage message to be sent
   */
  protected abstract void publishMessage(String streamId, AiMessage aiMessage);

  @Override
  public void respondWithUseMenu(AiSessionKey sessionKey, AiCommandMenu commandMenu,
      AiMessage message) {
    String response =
        String.format(AiConstants.NOT_COMMAND, message.getAiMessage()) + "<br/><hr/><b>"
            + AiConstants.MENU_TITLE + "</b><ul><li>" + commandMenu.toString()
            .replace("\n", "</li><li>") + "</li></ul>";
    response = response.replace("<li></li>", "");

    AiResponse aiResponse = new AiResponse(new AiMessage(response), sessionKey.getStreamId());
    respond(aiResponse);
  }

}
