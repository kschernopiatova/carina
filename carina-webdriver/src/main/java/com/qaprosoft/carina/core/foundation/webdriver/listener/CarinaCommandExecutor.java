/*******************************************************************************
 * Copyright 2020-2022 Zebrunner Inc (https://www.zebrunner.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.qaprosoft.carina.core.foundation.webdriver.listener;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qaprosoft.carina.core.foundation.commons.SpecialKeywords;
import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;
import com.qaprosoft.carina.core.foundation.utils.common.CommonUtils;

/**
 * EventFiringSeleniumCommandExecutor triggers event listener before/after execution of the command.
 */
public class CarinaCommandExecutor extends HttpCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    public CarinaCommandExecutor(URL addressOfRemoteServer) {
        super(addressOfRemoteServer);
    }

    @Override
    public Response execute(Command command) throws IOException {
        Response response = null;
        boolean isContinue = true;
        int retry = 10; //max attempts to repeit
        Number pause = Configuration.getInt(Parameter.EXPLICIT_TIMEOUT) / retry;
        while (retry > 0 && isContinue) {
            try {
                response = super.execute(command);
                isContinue = false;
            } catch (WebDriverException e) {
                //TODO: remove after debugging the issue
                e.printStackTrace();
                LOGGER.error("Temp CarinaCommandExecutor catched: ", e);
                
                String msg = e.getMessage();
                if (msg.contains(SpecialKeywords.DRIVER_CONNECTION_REFUSED)
                        || msg.contains(SpecialKeywords.DRIVER_CONNECTION_REFUSED2)) {
                    LOGGER.warn("Enabled command executor retries: " + msg);
                    CommonUtils.pause(pause);
                } else {
                    throw e;
                }
            } finally {
                retry--;
            }
        }

        return response;
    }

}