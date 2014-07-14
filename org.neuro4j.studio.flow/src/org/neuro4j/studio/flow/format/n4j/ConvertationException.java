/*
 * Copyright (c) 2013-2014, Neuro4j.org
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
package org.neuro4j.studio.flow.format.n4j;

public class ConvertationException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 8622249445842241152L;

    public ConvertationException() {
        super();
    }

    public ConvertationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConvertationException(String message) {
        super(message);
    }

    public ConvertationException(Throwable cause) {
        super(cause);
    }

}
