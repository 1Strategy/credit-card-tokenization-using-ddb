//   Copyright 2018 1Strategy, LLC

//     Licensed under the Apache License, Version 2.0 (the "License");
//     you may not use this file except in compliance with the License.
//     You may obtain a copy of the License at

//         http://www.apache.org/licenses/LICENSE-2.0

//     Unless required by applicable law or agreed to in writing, software
//     distributed under the License is distributed on an "AS IS" BASIS,
//     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//     See the License for the specific language governing permissions and
//     limitations under the License.

package com.onestrategy;


import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.AttributeEncryptor;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.encryption.DynamoDBEncryptor;
import com.amazonaws.services.dynamodbv2.datamodeling.encryption.providers.DirectKmsMaterialProvider;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;

public class AwsDynamodbKmsEncryptionProvider implements com.onestrategy.Crypto {

    private DynamoDBMapper _mapper;
    private AmazonDynamoDB _ddb;

    public AwsDynamodbKmsEncryptionProvider() {
        // Set up our configuration and clients
        _ddb = AmazonDynamoDBClientBuilder.standard().withRegion(EncryptionInfo.REGION).build();
        final AWSKMS kms = AWSKMSClientBuilder.standard().withRegion(EncryptionInfo.REGION).build();
        final DirectKmsMaterialProvider cmp = new DirectKmsMaterialProvider(kms, EncryptionInfo.CMK_ARN);
        // Encryptor creation
        final DynamoDBEncryptor encryptor = DynamoDBEncryptor.getInstance(cmp);
        // Mapper Creation
        // Please note the use of SaveBehavior.CLOBBER. Omitting this can result in data-corruption.
        DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder().withSaveBehavior(SaveBehavior.CLOBBER).build();
        _mapper = new DynamoDBMapper(_ddb, mapperConfig, new AttributeEncryptor(encryptor));
    }
	
    public String encrypt(String unencryptedString) {
        PaymentTransaction record = new PaymentTransaction();
        record.setPartitionAttribute(TokenProvider.getGlobalUniqueKey());
        record.setPANumber(unencryptedString);
        try {
            // Save it to DynamoDB
            _mapper.save(record);
            return record.getPartitionAttribute();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
	
    public String decrypt(String token) {
        try {
            PaymentTransaction pt = _mapper.load(PaymentTransaction.class, token);
            return pt.getPANumber();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Boolean delete(String token) {
        final Map<String, AttributeValue> record = new HashMap<>();
        record.put(EncryptionInfo.KEY_Attr_NAME, new AttributeValue().withS(token));
        try{
            _ddb.deleteItem(EncryptionInfo.TABLE_NAME, record);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @DynamoDBTable(tableName = EncryptionInfo.TABLE_NAME)
    public static final class PaymentTransaction {
      private String partitionAttribute;
      private String pANumber;
  
      @DynamoDBHashKey(attributeName = EncryptionInfo.KEY_Attr_NAME)
      public String getPartitionAttribute() {
        return partitionAttribute;
      }

      public void setPartitionAttribute(String partitionAttribute) {
        this.partitionAttribute = partitionAttribute;
      }
  
      @DynamoDBAttribute(attributeName = EncryptionInfo.PANUMBER_ATTR_NAME)
      public String getPANumber() {
        return pANumber;
      }
  
      public void setPANumber(String number) {
        this.pANumber = number;
      }
    }
}
