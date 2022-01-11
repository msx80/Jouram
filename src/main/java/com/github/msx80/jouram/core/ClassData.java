package com.github.msx80.jouram.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.msx80.jouram.Jouram;

/**
 * Stores all mutator method of a given class
 *
 */
public class ClassData {

	private final Map<Method, String> idByMethod = new HashMap<>();
	private final Map<String, Method> methodById = new HashMap<>();

	public ClassData(Class<?> interf) {
		
		// let's analyze the class
		for(Method m : interf.getMethods())
		{
			
			if(m.isAnnotationPresent(Mutator.class))
			{
				// if this method is marked Mutator, store it and its ID in the maps
				String methodId = getMethodId(m);
				LOG.info( "Identified mutator method {}",methodId);
				if(idByMethod.containsValue(methodId))
				{
					throw new JouramException("Duplicated methodId: "+methodId);
				}
				idByMethod.put(m, methodId);
				methodById.put(methodId, m);
			}
		}
    }
	
	private String getMethodId(Method m) {
		String k = m.getName();
		for (Class<?> p : m.getParameterTypes()) {
			k += ","+p.getName();
		}
		k+= "="+m.getReturnType().getName();
		
		return k;
	}


	private static Logger LOG = LoggerFactory.getLogger(Jouram.class);


	public String getIdByMethod(Method method) {
		return idByMethod.get(method);
	}

	public Method getMethodById(String methodId) {
		return methodById.get(methodId);
	}
	
}
