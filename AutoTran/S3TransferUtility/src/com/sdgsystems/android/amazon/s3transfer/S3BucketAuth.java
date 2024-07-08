/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.sdgsystems.android.amazon.s3transfer;

import java.io.Serializable;

public class S3BucketAuth implements Serializable {
    // You should replace these values with your own
    // See the readme for details on what to fill in
    public String AWS_ACCOUNT_ID = "331592269501";

    public String COGNITO_POOL_ID =
            "us-east-1:6ece6298-b343-4499-9533-db0d4130e8e1";
    
    public String COGNITO_ROLE_UNAUTH =
            "arn:aws:iam::331592269501:role/Cognito_AutoTranUnauth_DefaultRole";
    
    public String COGNITO_ROLE_AUTH =
    		"arn:aws:iam::331592269501:role/Cognito_AutoTranAuth_DefaultRole";
    
    // Note, the bucket will be created in all lower case letters
    // If you don't enter an all lower case title, any references you add
    // will need to be sanitized
    //public static final String BUCKET_NAME = "elasticbeanstalk-us-east-1-331592269501";
    public String BUCKET_NAME = "autotran-logs";
    
    public String folderName = "testFolder";
}
