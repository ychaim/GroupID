package org.symphonyoss.symphony.bots.helpdesk.bot.it;

import org.apache.commons.lang3.StringUtils;
import org.symphonyoss.client.model.Room;
import org.symphonyoss.symphony.clients.model.SymUser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by crepache on 22/02/18.
 * Class to store the variables to use on any part of tests.
 */
public class TestContext {

  private static TestContext instance;

  private Map<UsersEnum, SymUser> users = new HashMap();

  private Room queueRoom;

  private TestContext() {
  }

  public static synchronized TestContext getInstance() {
    if (instance == null) {
      instance = new TestContext();
    }

    return instance;
  }

  public SymUser getUser(UsersEnum key) {
    if (key == null) {
      return null;
    }

    return users.get(key);
  }

  public void setUsers(UsersEnum key, SymUser user) {
    users.put(key, user);
  }

  public Room getQueueRoom() {
    return queueRoom;
  }

  public void setQueueRoom(Room queueRoom) {
    this.queueRoom = queueRoom;
  }
}
