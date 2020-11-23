/*
 * Copyright (C) 2017 Samsung Electronics Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cloud.artik.example.oauth;

public class Config {
    //static final String CLIENT_ID = "YOUR_CLIENT_ID"; 91a27a61-fa97-4ebd-a8e2-021d11511066
    public static String CLIENT_ID = "920f6775-bd8a-4355-af77-76391a5ea21f";

    // MUST be consistent with "AUTH REDIRECT URL" of your application set up at the developer.artik.cloud
    //static final String REDIRECT_URI = "cloud.artik.example.oauth://oauth2callback";
    public static String REDIRECT_URI = "oauth2://sso8";

}
