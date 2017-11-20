package org.symphonyoss.symphony.bots.helpdesk.messageproxy.config;

/**
 * Created by nick.tarsillo on 11/13/17.
 */
public class MessageProxyServiceConfig {
  private String groupId;
  private String agentStreamId;
  private String claimMessageTemplate;
  private String claimEntityTemplate;
  private String claimEntityHostTemplate;
  private String claimEntityHeaderTemplate;
  private String claimEntityBodyTemplate;
  private String ticketCreationMessage;

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getAgentStreamId() {
    return agentStreamId;
  }

  public void setAgentStreamId(String agentStreamId) {
    this.agentStreamId = agentStreamId;
  }

  public String getClaimMessageTemplate() {
    return claimMessageTemplate;
  }

  public void setClaimMessageTemplate(String claimMessageTemplate) {
    this.claimMessageTemplate = claimMessageTemplate;
  }

  public String getClaimEntityTemplate() {
    return claimEntityTemplate;
  }

  public void setClaimEntityTemplate(String claimEntityTemplate) {
    this.claimEntityTemplate = claimEntityTemplate;
  }

  public String getTicketCreationMessage() {
    return ticketCreationMessage;
  }

  public void setTicketCreationMessage(String ticketCreationMessage) {
    this.ticketCreationMessage = ticketCreationMessage;
  }

  public String getClaimEntityHostTemplate() {
    return claimEntityHostTemplate;
  }

  public void setClaimEntityHostTemplate(String claimEntityHostTemplate) {
    this.claimEntityHostTemplate = claimEntityHostTemplate;
  }

  public String getClaimEntityHeaderTemplate() {
    return claimEntityHeaderTemplate;
  }

  public void setClaimEntityHeaderTemplate(String claimEntityHeaderTemplate) {
    this.claimEntityHeaderTemplate = claimEntityHeaderTemplate;
  }

  public String getClaimEntityBodyTemplate() {
    return claimEntityBodyTemplate;
  }

  public void setClaimEntityBodyTemplate(String claimEntityBodyTemplate) {
    this.claimEntityBodyTemplate = claimEntityBodyTemplate;
  }
}
