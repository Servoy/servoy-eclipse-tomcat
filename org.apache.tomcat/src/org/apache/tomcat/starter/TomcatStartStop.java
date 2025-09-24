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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Set;

import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.tomcat.Activator;
import org.apache.tomcat.OSGIWebappClassLoader;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.websocket.pojo.Constants;
import org.apache.tomcat.websocket.pojo.PojoMethodMapping;
import org.apache.tomcat.websocket.server.DefaultServerEndpointConfigurator;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import jakarta.websocket.server.ServerEndpointConfig.Configurator;


/**
 * Actual tomcat starter.
 * @author jcompagner
 */
public class TomcatStartStop
{
	private static Catalina catalina = null;

	public static void startTomcat(String dir)
	{
		TomcatURLStreamHandlerFactory.disable();
		System.setProperty("catalina.home", dir);
		System.setProperty("catalina.base", dir);

		catalina = new Catalina();
		String file = System.getProperty("webserver-file", null);
		if (file != null) catalina.setConfigFile(dir + "/conf/" + file);
		catalina.setParentClassLoader(new OSGIWebappClassLoader(TomcatStartStop.class.getClassLoader()));
		catalina.load();
		Service[] services = catalina.getServer().findServices();
		for (Service service : services)
		{
			Container[] containers = service.getContainer().findChildren();
			for (Container chost : containers)
			{
				chost.addLifecycleListener(new LifecycleListener()
				{

					@Override
					public void lifecycleEvent(LifecycleEvent event)
					{
						if (event.getType() == Lifecycle.AFTER_START_EVENT)
						{
							Container host = (Container)event.getSource();
							Container[] contexts = host.findChildren();
							for (Container container : contexts)
							{
								StandardContext sc = (StandardContext)container;
								sc.filterStop();
								ServletContext context = sc.getServletContext();
								ServerContainer serverContainer = (ServerContainer)context.getAttribute("jakarta.websocket.server.ServerContainer");
								Set<ServletInstance> servletInstances = Activator.getActivator().getServletInstances(context.getContextPath());
								for (ServletInstance servletInstance : servletInstances)
								{
									try
									{
										Servlet servlet = servletInstance.getServletInstance();
										Wrapper wrapper = sc.createWrapper();
										wrapper.setName(servlet.getClass().getSimpleName());
										wrapper.setServletClass(servlet.getClass().getName());
										wrapper.setServlet(servlet);
										wrapper.setAsyncSupported(true);
										sc.addChild(wrapper);
										wrapper.addMapping(servletInstance.getUrlPattern());
									}
									catch (Exception e)
									{
										e.printStackTrace();
									}
								}

								Set<Class< ? >> annotatedClasses = Activator.getActivator().getAnnotatedClasses(context.getContextPath());
								for (Class< ? > cls : annotatedClasses)
								{
									ServerEndpoint serverEndpoint = cls.getAnnotation(ServerEndpoint.class);
									if (serverEndpoint != null)
									{
										try
										{
											String path = serverEndpoint.value();

											// Method mapping
											PojoMethodMapping methodMapping = new PojoMethodMapping(cls, Arrays.asList(serverEndpoint.decoders()), path,
												sc.getInstanceManager());

											// ServerEndpointConfig
											ServerEndpointConfig sec;
											Class< ? extends Configurator> configuratorClazz = serverEndpoint.configurator();
											Configurator configurator = null;
											if (configuratorClazz.equals(Configurator.class))
											{
												configuratorClazz = DefaultServerEndpointConfigurator.class;
											}
											try
											{
												configurator = configuratorClazz.getDeclaredConstructor().newInstance();
											}
											catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
												| NoSuchMethodException | SecurityException e)
											{
												throw new DeploymentException("serverContainer.configuratorFail", e);
											}
											sec = ServerEndpointConfig.Builder.create(cls, path).decoders(Arrays.asList(serverEndpoint.decoders())).encoders(
												Arrays.asList(serverEndpoint.encoders())).subprotocols(
													Arrays.asList(serverEndpoint.subprotocols()))
												.configurator(configurator).build();
											sec.getUserProperties().put(Constants.POJO_METHOD_MAPPING_KEY, methodMapping);

											serverContainer.addEndpoint(sec);
										}
										catch (DeploymentException e)
										{
											e.printStackTrace();
										}
									}
									else
									{
										WebFilter webFilter = cls.getAnnotation(WebFilter.class);
										if (webFilter != null)
										{
											String name = webFilter.filterName();
											if (name == null || name.equals("")) name = cls.getName();
											try
											{
												Filter filter = (Filter)cls.getDeclaredConstructor().newInstance();
												FilterDef filterDef = new FilterDef();
												filterDef.setFilterName(name);
												filterDef.setFilter(filter);
												filterDef.setFilterClass(cls.getName());
												for (WebInitParam param : webFilter.initParams())
												{
													filterDef.addInitParameter(param.name(), param.value());
												}
												sc.addFilterDef(filterDef);
												FilterMap map = new FilterMap();
												for (DispatcherType type : webFilter.dispatcherTypes())
												{
													map.setDispatcher(type.name());
												}
												map.setFilterName(name);
												String[] urlPatterns = webFilter.urlPatterns();
												for (String urlPattern : urlPatterns)
												{
													map.addURLPattern(urlPattern);
												}
												sc.addFilterMap(map);
											}
											catch (Exception e)
											{
												e.printStackTrace();
											}

										}
										else
										{
											WebServlet webServlet = cls.getAnnotation(WebServlet.class);
											if (webServlet != null)
											{
												String name = webServlet.name();
												if (name == null || name.equals("")) name = cls.getName();
												try
												{
													Servlet servlet = (Servlet)cls.getDeclaredConstructor().newInstance();
													Wrapper wrapper = sc.createWrapper();
													wrapper.setName(name);
													wrapper.setServletClass(cls.getName());
													wrapper.setServlet(servlet);
													sc.addChild(wrapper);
													for (String mapping : webServlet.urlPatterns())
													{
														wrapper.addMapping(mapping);
													}
													for (String mapping : webServlet.value())
													{
														wrapper.addMapping(mapping);
													}
													for (WebInitParam param : webServlet.initParams())
													{
														wrapper.addInitParameter(param.name(), param.value());
													}
												}
												catch (Exception e)
												{
													e.printStackTrace();
												}
											}
										}
									}

								}
								sc.filterStart();
							}
						}
					}
				});
			}
		}
		catalina.start();
		Activator.getActivator().getStartedListeners().forEach(listener -> listener.started());
		System.setProperty("svy.tomcat.started", "true");
	}

	public static void stop()
	{
		try
		{
			if (catalina != null) catalina.stop();
		}
		catch (Throwable ex)
		{
			//fail silently
		}
	}
}
