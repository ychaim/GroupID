import { approveAttachment, denyAttachment } from '../api/apiCalls';

export default class AttachmentService {
  constructor(serviceName) {
    this.serviceName = serviceName;
  }

  approve(message) {
    return approveAttachment(message)
      .catch((error) => {
        switch (error.message) {
          case '': {
            break;
          }
          default: {
            break;
          }
        }
      });
  }

  deny(message) {
    return denyAttachment(message)
      .catch((error) => {
        switch (error.message) {
          case '': {
            break;
          }
          default: {
            break;
          }
        }
      });
  }

}