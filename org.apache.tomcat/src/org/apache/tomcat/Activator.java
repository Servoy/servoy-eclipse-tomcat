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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;
import org.apache.tomcat.starter.ITomcatStartedListener;
import org.apache.tomcat.starter.TomcatStartStop;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator to bring tomcat up.
 * @author jcompagner
 */
public class Activator implements BundleActivator
{
	private static Activator self;

	private final Set<IServicesProvider> serviceProviders = new HashSet<IServicesProvider>();
	private final Set<ITomcatStartedListener> startedListeners = new HashSet<ITomcatStartedListener>();
	private BundleContext context;

	public static Activator getActivator()
	{
		return self;
	}

	public BundleContext getContext()
	{
		return context;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception
	{
		self = this;
		context = bundleContext;

		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint(IServicesProvider.EXTENSION_ID);
		IExtension[] extensions = ep.getExtensions();
		if (extensions != null && extensions.length > 0)
		{
			for (IExtension extension : extensions)
			{
				IServicesProvider provider = (IServicesProvider)extension.getConfigurationElements()[0].createExecutableExtension("class");
				provider.registerServices();
				serviceProviders.add(provider);
			}
		}

		ep = reg.getExtensionPoint(ITomcatStartedListener.EXTENSION_ID);
		extensions = ep.getExtensions();
		if (extensions != null && extensions.length > 0)
		{
			for (IExtension extension : extensions)
			{
				ITomcatStartedListener startedListener = (ITomcatStartedListener)extension.getConfigurationElements()[0].createExecutableExtension("class");
				startedListeners.add(startedListener);
			}
		}
	}

	public Set<Class< ? >> getAnnotatedClasses(String ctx)
	{
		Set<Class< ? >> classes = new HashSet<Class< ? >>();
		for (IServicesProvider serviceProvider : serviceProviders)
		{
			Set<Class< ? >> annotatedClasses = serviceProvider.getAnnotatedClasses(ctx);
			if (annotatedClasses != null) classes.addAll(annotatedClasses);
		}
		return classes;
	}

	/**
	 * @return the startedListeners
	 */
	public Set<ITomcatStartedListener> getStartedListeners()
	{
		return Collections.unmodifiableSet(startedListeners);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception
	{
		TomcatStartStop.stop();
		context = null;
		self = null;
	}
}
