package org.symphonyoss.symphony.bots.helpdesk.service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.symphonyoss.symphony.bots.helpdesk.service.api.MembershipApi;
import org.symphonyoss.symphony.bots.helpdesk.service.api.TicketApi;
import org.symphonyoss.symphony.bots.helpdesk.service.config.HelpDeskServiceConfig;
import org.symphonyoss.symphony.bots.helpdesk.service.model.Membership;

/**
 * Created by nick.tarsillo on 9/26/17.
 * Service for managing and getting memberships of users with bot.
 */
public class MembershipClient {
  private static final Logger LOG = LoggerFactory.getLogger(MembershipClient.class);

  public enum MembershipType {
    CLIENT("CLIENT"),
    AGENT("AGENT");

    private String type;
    MembershipType(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }
  }

  private MembershipApi membershipApi;
  private String groupId;

  public MembershipClient(String groupId, String memberServiceUrl) {
    ApiClient apiClient = Configuration.getDefaultApiClient();
    apiClient.setBasePath(memberServiceUrl);
    membershipApi = new MembershipApi(apiClient);
    this.groupId = groupId;
  }

  /**
   * Get the membership of a user.
   * @param userId the user id of the user.
   * @return
   */
  public Membership getMembership(String userId) {
    try {
      return membershipApi.getMembership(userId, groupId).getMembership();
    } catch (ApiException e) {
      LOG.error("Failed to get membership: ", e);
    }

    return null;
  }

  /**
   * Create a new membership for a user. (CLIENT by default.)
   * @param userId the user id to create a membership for.
   * @return the new membership
   */
  public Membership newMembership(String userId) {
    Membership membership = new Membership();
    membership.setId(userId);
    membership.setGroupId(groupId);
    membership.setType(MembershipType.CLIENT.getType());
    try {
      Membership newMembership = membershipApi.createMembership(membership).getMembership();
      LOG.info("Created new client membership for userid: " + userId);
      return newMembership;
    } catch (ApiException e) {
      LOG.error("Failed to create new membership for user: ", e);
    }
    return membership;
  }

  /**
   * Promotes a user to have an AGENT level membership.
   * @param membership the membership to update.
   * @return the updated membership
   */
  public Membership updateMembership(Membership membership) {
    try {
      Membership updateMembership = membershipApi.updateMembership(membership.getId(), groupId, membership).getMembership();
      LOG.info("Promoted client to agent for userid: " + membership.getId());
      return updateMembership;
    } catch (ApiException e) {
      LOG.error("Could not update membership", e);
    }

    return null;
  }
}