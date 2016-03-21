/*******************************************************************************
 * Copyright 2016 Natan Guilherme.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.backends.dragome;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.events.Event;

import com.badlogic.gdx.backends.dragome.js.storage.Storage;
import com.badlogic.gdx.backends.dragome.js.typedarrays.ArrayBuffer;
import com.badlogic.gdx.backends.dragome.js.typedarrays.ArrayBufferView;
import com.badlogic.gdx.backends.dragome.js.typedarrays.Float32Array;
import com.badlogic.gdx.backends.dragome.js.typedarrays.Float64Array;
import com.badlogic.gdx.backends.dragome.js.typedarrays.Int16Array;
import com.badlogic.gdx.backends.dragome.js.typedarrays.Int32Array;
import com.badlogic.gdx.backends.dragome.js.typedarrays.Int8Array;
import com.badlogic.gdx.backends.dragome.js.typedarrays.TypedArray;
import com.badlogic.gdx.backends.dragome.js.typedarrays.Uint16Array;
import com.badlogic.gdx.backends.dragome.js.typedarrays.Uint32Array;
import com.badlogic.gdx.backends.dragome.js.typedarrays.Uint8Array;
import com.badlogic.gdx.backends.dragome.js.typedarrays.Uint8ClampedArray;
import com.badlogic.gdx.backends.dragome.js.webgl.WebGLActiveInfo;
import com.badlogic.gdx.backends.dragome.js.webgl.WebGLBuffer;
import com.badlogic.gdx.backends.dragome.js.webgl.WebGLContextAttributes;
import com.badlogic.gdx.backends.dragome.js.webgl.WebGLFramebuffer;
import com.badlogic.gdx.backends.dragome.js.webgl.WebGLObject;
import com.badlogic.gdx.backends.dragome.js.webgl.WebGLProgram;
import com.badlogic.gdx.backends.dragome.js.webgl.WebGLRenderbuffer;
import com.badlogic.gdx.backends.dragome.js.webgl.WebGLRenderingContext;
import com.badlogic.gdx.backends.dragome.js.webgl.WebGLShader;
import com.badlogic.gdx.backends.dragome.js.webgl.WebGLTexture;
import com.badlogic.gdx.backends.dragome.js.webgl.WebGLUniformLocation;
import com.dragome.commons.ChainedInstrumentationDragomeConfigurator;
import com.dragome.commons.DragomeConfiguratorImplementor;
import com.dragome.commons.compiler.ClasspathFile;
import com.dragome.commons.compiler.InMemoryClasspathFile;
import com.dragome.commons.compiler.annotations.CompilerType;
import com.dragome.web.config.NodeSubTypeFactory;
import com.dragome.web.enhancers.jsdelegate.DefaultDelegateStrategy;
import com.dragome.web.enhancers.jsdelegate.JsDelegateGenerator;
import com.dragome.web.enhancers.jsdelegate.interfaces.SubTypeFactory;
import com.dragome.web.helpers.serverside.DefaultClasspathFilter;
import com.dragome.web.html.dom.html5canvas.interfaces.CanvasImageSource;
import com.dragome.web.html.dom.html5canvas.interfaces.CanvasRenderingContext2D;
import com.dragome.web.html.dom.html5canvas.interfaces.HTMLCanvasElement;
import com.dragome.web.html.dom.html5canvas.interfaces.ImageElement;

/** @author xpenatan */
@DragomeConfiguratorImplementor(priority= 10)
public class DragomeConfiguration extends ChainedInstrumentationDragomeConfigurator
{
	HashSet<String> paths;
	String projName;
	String projPath;
	List<ClasspathFile> extraClasspathFiles;
	private JsDelegateGenerator jsDelegateGenerator;

	boolean cache= false;

	public DragomeConfiguration()
	{

		// System.setProperty("dragome-compile-mode", CompilerMode.Production.toString());
		// System.setProperty("dragome-compile-mode", CompilerMode.Debug.toString());

		extraClasspathFiles= new ArrayList<ClasspathFile>();
		projPath= System.getProperty("user.dir");
		File file= new File(projPath);
		projName= file.getName();
		paths= new HashSet<String>();
		setClasspathFilter(new DefaultClasspathFilter()
		{
			public boolean accept(File pathname)
			{
				boolean accept= super.accept(pathname);
				String absolutePath= pathname.getAbsolutePath();

				if (absolutePath.contains("utils\\Json"))
					return false;

				String className= pathname.getName().replace(".class", "");
				if (paths.contains(className))
					return false;

				if (absolutePath.contains("gdx-backend-dragome"))
				{
					paths.add(className);
				}

				System.out.println("ClassPathFilter: " + accept + " - " + absolutePath);

				return accept;
			}

		});

	}

	public void add(Class<?> clazz)
	{
		byte[] bytecode= jsDelegateGenerator.generate(clazz);
		String classname= JsDelegateGenerator.createDelegateClassName(clazz.getName());
		addClassBytecode(bytecode, classname);
		extraClasspathFiles.add(new InMemoryClasspathFile(classname, bytecode));
	}

	private void createJsDelegateGenerator(String classpath)
	{
		jsDelegateGenerator= new JsDelegateGenerator(classpath.replace(";", ":"), new DefaultDelegateStrategy()
		{
			public String createMethodCall(Method method, String params)
			{
				if (params == null)
					params= "";

				String name= method.getName();
				Class<?>[] superclass= method.getDeclaringClass().getInterfaces();
				if (superclass.length > 0 && superclass[0].equals(TypedArray.class))
				{
					if (name.equals("set") && method.getParameterTypes().length == 2 && method.getParameterTypes()[0].equals(int.class))
						return "this.node[$1] = $2";
					else if ((name.equals("get") || name.equals("getAsDouble")) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(int.class))
						return "this.node[$1]";
				}

				return super.createMethodCall(method, params);
			}

			public String getSubTypeExtractorFor(Class<?> interface1, String methodName)
			{
				if (methodName.equals("item") || methodName.equals("cloneNode"))
					return "temp.nodeType";

				return null;
			}

			public Class<? extends SubTypeFactory> getSubTypeFactoryClassFor(Class<?> interface1, String methodName)
			{
				if (methodName.equals("item") || methodName.equals("cloneNode"))
					return NodeSubTypeFactory.class;

				return null;
			}
		});
	}

	public List<ClasspathFile> getExtraClasspath(String classpath)
	{
		if (jsDelegateGenerator == null)
			createJsDelegateGenerator(classpath);

		add(Document.class);
		add(Element.class);
		add(Attr.class);
		add(NodeList.class);
		add(Node.class);
		add(NamedNodeMap.class);
		add(Text.class);
		add(HTMLCanvasElement.class);
		add(CanvasRenderingContext2D.class);
		add(CanvasImageSource.class);
		add(ImageElement.class);
		add(Event.class);

		add(WebGLActiveInfo.class);
		add(WebGLBuffer.class);
		add(WebGLContextAttributes.class);
		add(WebGLFramebuffer.class);
		add(WebGLObject.class);
		add(WebGLProgram.class);
		add(WebGLRenderbuffer.class);
		add(WebGLRenderingContext.class);
		add(WebGLShader.class);
		add(WebGLTexture.class);
		add(WebGLUniformLocation.class);

		add(ArrayBuffer.class);
		add(ArrayBufferView.class);

		// add(DataView.class);
		// add(DataViewStream.class);
		add(Float32Array.class);
		add(Float64Array.class);
		add(Int16Array.class);
		add(Int32Array.class);
		add(Int8Array.class);
		add(Uint16Array.class);
		add(Uint32Array.class);
		add(Uint8Array.class);
		add(Uint8ClampedArray.class);

		add(Storage.class);
		add(TypedArraysFactory.class);

		return extraClasspathFiles;
	}

	@Override
	public boolean filterClassPath(String aClassPathEntry)
	{
		boolean flag= false;
		if (aClassPathEntry.contains(projName))
			flag= true;
		else if (aClassPathEntry.contains("repository") && aClassPathEntry.contains("gdx") || aClassPathEntry.contains("gdx.jar") || aClassPathEntry.contains("gdx\\bin"))
			flag= true;
		else if (aClassPathEntry.contains("repository\\gdx-backend-dragome") || aClassPathEntry.contains("gdx-backend-dragome\\bin"))
			flag= true;
		else if (aClassPathEntry.contains("dragome-js-commons-") || aClassPathEntry.contains("dragome-js-commons\\bin"))
			flag= true;
		else if (aClassPathEntry.contains("dragome-js-jre-") || aClassPathEntry.contains("dragome-js-jre\\bin"))
			flag= true;
		else if (aClassPathEntry.contains("dragome-callback-evictor-") || aClassPathEntry.contains("dragome-callback-evictor\\bin"))
			flag= true;
		else if (aClassPathEntry.contains("dragome-form-bindings-") || aClassPathEntry.contains("dragome-form-bindings\\bin"))
			flag= true;
		else if (aClassPathEntry.contains("dragome-core-") || aClassPathEntry.contains("dragome-core\\bin"))
			flag= true;
		else if (aClassPathEntry.contains("dragome-method-logger-") || aClassPathEntry.contains("dragome-method-logger\\bin"))
			flag= true;
		else if (aClassPathEntry.contains("dragome-web-") || aClassPathEntry.contains("dragome-web\\bin"))
			flag= true;
		else if (aClassPathEntry.contains("dragome-guia-web-") || aClassPathEntry.contains("dragome-guia-web\\bin"))
			flag= true;
		else if (aClassPathEntry.contains("dragome-guia-") || aClassPathEntry.contains("dragome-guia\\bin"))
			flag= true;
		System.out.println("flag: " + flag + " path: " + aClassPathEntry);
		return flag;
	}

	public CompilerType getDefaultCompilerType()
	{
		return CompilerType.Standard;
	}

	@Override
	public boolean isCheckingCast()
	{
		return false;
	}
}
