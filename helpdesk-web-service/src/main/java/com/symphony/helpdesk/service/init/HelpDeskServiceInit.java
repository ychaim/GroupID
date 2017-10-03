package com.symphony.helpdesk.service.init;

import com.symphony.api.helpdesk.service.api.factories.V1ApiServiceFactory;
import com.symphony.helpdesk.service.api.V1HelpDeskApi;
import com.symphony.helpdesk.service.config.ServiceConfig;
import com.symphony.helpdesk.service.model.MembershipSQLService;
import com.symphony.helpdesk.service.model.SQLConnection;
import com.symphony.helpdesk.service.model.TicketSQLService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;

/**
 * Created by nick.tarsillo on 9/25/17.
 */
public class HelpDeskServiceInit extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(HelpDeskServiceInit.class);

  @Override
  public void init(ServletConfig config){
    ServiceConfig.init();

    LOG.info("HelpDesk Service starting...");

    SQLConnection sqlConnection = new SQLConnection(
        System.getProperty(ServiceConfig.DATABASE_DRIVER),
        System.getProperty(ServiceConfig.DATABASE_URL),
        System.getProperty(ServiceConfig.DATABASE_USER),
        System.getProperty(ServiceConfig.DATABASE_PASSWORD));
    MembershipSQLService membershipSQLService = new MembershipSQLService(sqlConnection,
        System.getProperty(ServiceConfig.MEMBERSHIP_TABLE_NAME));
    TicketSQLService ticketSQLService = new TicketSQLService(sqlConnection,
        System.getProperty(ServiceConfig.TICKET_TABLE_NAME));
    V1HelpDeskApi v1HelpDeskApi = new V1HelpDeskApi(membershipSQLService, ticketSQLService);
    V1ApiServiceFactory.setService(v1HelpDeskApi);

    LOG.info("HelpDesk Service is ready to go!");
  }

}
