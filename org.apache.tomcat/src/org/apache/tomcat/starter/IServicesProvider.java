/*
 * Copyright (C) 2014 Servoy BV
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

package org.apache.tomcat.starter;

import java.util.Set;

/**
 * Extension point interface.
 * @author jcompagner
 */
public interface IServicesProvider
{
	static final String EXTENSION_ID = "org.apache.tomcat.serviceprovider";

	default void registerServices()
	{
	}

	/**
	 * @param context The context that is being configured
	 */
	default Set<Class< ? >> getAnnotatedClasses(String context)
	{
		return null;
	}

	/**
	 * @param context The context that is being configured
	 */
	default Set<ServletInstance> getServletInstances(String context)
	{
		return null;
	}
}
