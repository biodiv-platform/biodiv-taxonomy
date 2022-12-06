/**
 * 
 */
package com.strandls.taxonomy.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.mail_utility.model.EnumModel.FIELDS;
import com.strandls.mail_utility.model.EnumModel.INFO_FIELDS;
import com.strandls.mail_utility.model.EnumModel.MAIL_TYPE;
import com.strandls.mail_utility.model.EnumModel.PERMISSION_GRANT;
import com.strandls.mail_utility.model.EnumModel.PERMISSION_REQUEST;
import com.strandls.mail_utility.producer.RabbitMQProducer;
import com.strandls.mail_utility.util.JsonUtil;
import com.strandls.taxonomy.RabbitMqConnection;
import com.strandls.user.pojo.User;

/**
 * @author Abhishek Rudra
 *
 * 
 */
public class MailUtils {

	private final Logger logger = LoggerFactory.getLogger(MailUtils.class);

	@Inject
	private RabbitMQProducer mailProducer;

	public void sendPermissionRequest(List<User> requestors, String taxonName, Long taxonId, String role,
			User requestee, String encryptedKey, String requestorMessage) {
		List<String> emailList = new ArrayList<String>();
		for (User requestor : requestors) {
			if (requestor.getEmail() != null)
				emailList.add(requestor.getEmail());
		}
		try {
			Map<String, Object> data = new HashMap<>();
			data.put(FIELDS.TO.getAction(), emailList.toArray());
			data.put(FIELDS.SUBSCRIPTION.getAction(), true);
			Map<String, Object> permissionRequest = new HashMap<>();

			permissionRequest.put(PERMISSION_REQUEST.ENCRYPTED_KEY.getAction(), encryptedKey);
			permissionRequest.put(PERMISSION_REQUEST.REQUESTEE_ID.getAction(), requestee.getId());
			permissionRequest.put(PERMISSION_REQUEST.REQUESTEE_NAME.getAction(), requestee.getName());
			permissionRequest.put(PERMISSION_REQUEST.ROLE.getAction(), role);
			permissionRequest.put(PERMISSION_REQUEST.TAXON_ID.getAction(), taxonId);
			permissionRequest.put(PERMISSION_REQUEST.TAXON_NAME.getAction(), taxonName);
			permissionRequest.put(PERMISSION_REQUEST.REQUESTOR_MESSAGE.getAction(), requestorMessage);

			data.put(FIELDS.DATA.getAction(), JsonUtil.unflattenJSON(permissionRequest));

			Map<String, Object> mData = new HashMap<>();
			mData.put(INFO_FIELDS.TYPE.getAction(), MAIL_TYPE.PERMISSION_REQUEST.getAction());
			mData.put(INFO_FIELDS.RECIPIENTS.getAction(), Arrays.asList(data));

			mailProducer.produceMail(RabbitMqConnection.EXCHANGE_BIODIV, RabbitMqConnection.MAIL_ROUTING_KEY, null,
					JsonUtil.mapToJSON(mData));

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

	public void sendPermissionGrant(User requestee, String taxonName, String role, Long taxonId) {

		if (requestee.getEmail() != null) {
			try {
				Map<String, Object> data = new HashMap<>();
				data.put(FIELDS.TO.getAction(), new String[] { requestee.getEmail() });
				data.put(FIELDS.SUBSCRIPTION.getAction(), true);

				Map<String, Object> permissionGrantData = new HashMap<>();
				permissionGrantData.put(PERMISSION_GRANT.REQUESTEE_NAME.getAction(), requestee.getName());
				permissionGrantData.put(PERMISSION_GRANT.ROLE.getAction(), role);
				permissionGrantData.put(PERMISSION_GRANT.TAXON_ID.getAction(), taxonId);
				permissionGrantData.put(PERMISSION_GRANT.TAXON_NAME.getAction(), taxonName);

				data.put(FIELDS.DATA.getAction(), JsonUtil.unflattenJSON(permissionGrantData));

				Map<String, Object> mData = new HashMap<>();
				mData.put(INFO_FIELDS.TYPE.getAction(), MAIL_TYPE.PERMISSION_GRANTED.getAction());
				mData.put(INFO_FIELDS.RECIPIENTS.getAction(), Arrays.asList(data));

				mailProducer.produceMail(RabbitMqConnection.EXCHANGE_BIODIV, RabbitMqConnection.MAIL_ROUTING_KEY, null,
						JsonUtil.mapToJSON(mData));
			} catch (Exception e) {
				logger.error(e.getMessage());
			}

		}

	}

}
