import MessageEnricher from './messageEnricher';
import actionFactory from '../utils/actionFactory';

const actions = require('../templates/attachmentActions.hbs');

const enricherServiceName = 'helpdesk-action-attachment-enricher';
const messageEvents = [
  'com.symphony.bots.helpdesk.event.makerchecker.action.performed',
];

export default class ActionAttachmentEnricher extends MessageEnricher {
  constructor(application) {
    super(enricherServiceName, messageEvents, application);
  }

  enrich(type, entity) {
    const actionPerformed = entity.state === 'APPROVED' || entity.state === 'DENIED';

    if (actionPerformed) {
      const data = actionFactory([], enricherServiceName, entity);
      const entityRegistry = SYMPHONY.services.subscribe('entity');
      const displayName = entity.checker !== null && entity.checker !== undefined ? entity.checker.displayName : '';
      const template = actions({ showActions: false,
        isApproved: entity.state === 'APPROVED',
        userName: displayName });

      entityRegistry.updateEnricher(entity.makerCheckerId, template, data);
    }
  }

  action() {
  }
}
