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

package org.apache.tomcat;

import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Helper classloader.
 * @author jcompagner
 */
public class OSGIWebappClassLoader extends ClassLoader
{
	// should this be an URLClassLoader ? see org.apache.catalina.loader.Webapploader buildClassPath ; should we add the bundles to classpath ?
	private final CopyOnWriteArrayList<Bundle> usedBundles = new CopyOnWriteArrayList<Bundle>();

	public OSGIWebappClassLoader()
	{
		super();
	}

	public OSGIWebappClassLoader(ClassLoader parent)
	{
		super(parent);
	}

	@Override
	public Class< ? > findClass(String name) throws ClassNotFoundException
	{
		try
		{
			return super.findClass(name);
		}
		catch (ClassNotFoundException e)
		{
			ServiceReference< ? > serviceReference = Activator.getActivator().getContext().getServiceReference(name);
			if (serviceReference != null)
			{
				usedBundles.add(serviceReference.getBundle());
				return serviceReference.getBundle().loadClass(name);
			}
			else
			{
				// now try to resolve it through the bundles that did provide a service.
				for (Bundle bundle : usedBundles)
				{
					try
					{
						return bundle.loadClass(name);
					}
					catch (ClassNotFoundException ex)
					{
						// ignore
					}
				}
			}
		}
		throw new ClassNotFoundException(name);
	}
}
