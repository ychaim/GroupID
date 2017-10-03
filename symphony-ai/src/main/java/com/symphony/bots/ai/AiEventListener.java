package com.symphony.bots.ai;

import com.symphony.bots.ai.model.AiConversation;
import com.symphony.bots.ai.model.AiMessage;
import com.symphony.bots.ai.model.AiSessionContext;

/**
 * Created by nick.tarsillo on 8/20/17.
 */
public interface AiEventListener {

  /**
   * Interprets an ai message, as a command line command.
   */
  void onCommand(AiMessage command, AiSessionContext sessionContext);

  /**
   * Interprets an ai message as a part of a contextual conversation with the ai.
   */
  void onConversation(AiMessage message, AiConversation aiConversation);
}
