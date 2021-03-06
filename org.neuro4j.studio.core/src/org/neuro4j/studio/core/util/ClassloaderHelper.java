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
package org.neuro4j.studio.core.util;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;




public class ClassloaderHelper
{
    private static String currentJavaProject;
    private static IResource currentResource;

    private static Map<String, URLClassLoader> loaders = new HashMap<String, URLClassLoader>();

    private static List<IJavaProject> getAllProjects() throws Exception
    {
        List<IJavaProject> javaProjects = new ArrayList<IJavaProject>();
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects) {
        	try{
                project.open(null /* IProgressMonitor */);
                IJavaProject javaProject = JavaCore.create(project);
                javaProjects.add(javaProject);        		
        	}catch(Exception ex){
        		System.err.println(ex.getMessage());
        	}

        }
        return javaProjects;
    }

    public static InputStream loadImage(String name)
    {
        String project = getActiveProjectName();

        if (!loaders.containsKey(project))
        {
            load(project);
        }

        URLClassLoader loader = loaders.get(project);
        if (loader != null){
            InputStream clazz = loader.getResourceAsStream(name);
            if (clazz != null)
            {
                return clazz;
            }
          
        }

        return null;
    }

    private static synchronized void load(String name)
    {

        if (loaders.containsKey(name))
        {
            return;
        }
        List<IJavaProject> javaProjects = null;

        try {
            javaProjects = getAllProjects();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return;
        }
        for (IJavaProject proj : javaProjects) {
            try {
                if (!proj.getProject().getName().equals(name))
                {
                    continue;
                }
                ClassLoader parentClassLoader = proj.getClass().getClassLoader();
                URL urls[] = getClasspathAsURLArray(proj);
                loaders.put(proj.getProject().getName(), new URLClassLoader(urls, parentClassLoader));
                return;
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }

    }

    public static Object getInstance(IJavaProject project, String fqClassname) throws Exception
    {
        if (!loaders.containsKey(project.getProject().getName()))
        {
            load(project.getProject().getName());
        }

        URLClassLoader loader = loaders.get(project.getProject().getName());
        if (loader == null)
        {
            return null;
        }

        try {
            Class<?> clazz = loader.loadClass(fqClassname);
            if (!clazz.isInterface() && !clazz.isEnum()) {
                return clazz.newInstance();
            } else {
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            // LogHelper.error("no class found for " + fqClassname);
        }

        return null;
    }

    public static Class<?> getClazz(String project, String fqClassname)
    {

        if (!loaders.containsKey(project))
        {
            load(project);
        }

        for (ClassLoader loader : loaders.values()) {
            try {

                Class<?> clazz = loader.loadClass(fqClassname);
                if (!clazz.isInterface() && !clazz.isEnum()) {
                    return clazz;
                } else {
                    return clazz;
                }
            } catch (Exception e) {
                System.err.println("no class found for " + fqClassname);
            }
        }

        return null;
    }
    
    

    public static URL[] getClasspathAsURLArray(IJavaProject javaProject)
    {
        if (javaProject == null)
            return null;
        Set<IJavaProject> visited = new HashSet<IJavaProject>();
        List<URL> urls = new ArrayList<URL>(20);
        collectClasspathURLs(javaProject, urls, visited, true);
        URL[] result = new URL[urls.size()];
        urls.toArray(result);
        return result;
    }

    public static List<IPath> getClasspathAsIPathArray(IJavaProject javaProject)
    {
        if (javaProject == null)
            return null;
        Set<IJavaProject> visited = new HashSet<IJavaProject>();
        List<IPath> urls = new ArrayList<IPath>(20);
        collectClasspathIPath(javaProject, urls, visited, true);
        return urls;
    }

    public static List<IPath> getClasspathAsURLArrayForWorkspace()
    {
        Set<URL> urls = new HashSet<URL>();
        List<IJavaProject> javaProjects;
        try {
            javaProjects = getAllProjects();
            for (IJavaProject proj : javaProjects) {
                List<IPath> s = getClasspathAsIPathArray(proj);
                if (s != null && !s.isEmpty())
                {
                    return s;
                }

            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    private static void collectClasspathURLs(IJavaProject javaProject, List<URL> urls, Set<IJavaProject> visited,
            boolean isFirstProject)
    {
        if (visited.contains(javaProject))
            return;
        visited.add(javaProject);
        IPath outPath = getJavaProjectOutputAbsoluteLocation(javaProject.getProject());
        if (outPath != null) {
            outPath = outPath.addTrailingSeparator();
            URL out = createFileURL(outPath);
            urls.add(out);

        }

        IClasspathEntry[] entries = null;
        try {
            entries = javaProject.getResolvedClasspath(true);
        } catch (JavaModelException e) {
            return;
        }

        IClasspathEntry entry;
        for (int i = 0; i < entries.length; i++) {
            entry = entries[i];
            switch (entry.getEntryKind()) {
                case IClasspathEntry.CPE_LIBRARY:
				try {
					urls.add(resolvePath(entry.getPath()));
				} catch (MalformedURLException e1) {
				} 
                    break;
                case IClasspathEntry.CPE_CONTAINER:
                case IClasspathEntry.CPE_VARIABLE:
                    collectClasspathEntryURL(entry, urls);
                    break;
                case IClasspathEntry.CPE_SOURCE:
                    IPath p = entry.getOutputLocation(); 
                    if (p != null)
						try {
							urls.add(resolvePath(p));
						} catch (MalformedURLException e) {

						} 
                    break; 
                case IClasspathEntry.CPE_PROJECT: {
                    if (isFirstProject || entry.isExported())
                        collectClasspathURLs(getJavaProject(entry), urls, visited, false);
                    break;
                }
            }
        }
    }
    public static URL resolvePath(IPath path) throws MalformedURLException 
    { 
     IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot(); 
     IResource res = root.findMember(path); 
     if (res != null) 
      path = res.getLocation(); 
      
     return path.toFile().toURI().toURL(); 
    } 
    private static void collectClasspathIPath(IJavaProject javaProject, List<IPath> urls, Set<IJavaProject> visited,
            boolean isFirstProject)
    {
        if (visited.contains(javaProject))
            return;
        visited.add(javaProject);

        IClasspathEntry[] entries = null;
        try {
            entries = javaProject.getResolvedClasspath(true);
        } catch (JavaModelException e) {
            return;
        }
        IClasspathEntry entry;
        for (int i = 0; i < entries.length; i++) {
            entry = entries[i];
            switch (entry.getEntryKind()) {
                case IClasspathEntry.CPE_LIBRARY:
                    addJarToList(entry.getPath(), urls);
                    break;
                case IClasspathEntry.CPE_CONTAINER:
                case IClasspathEntry.CPE_SOURCE:
                    break;
                case IClasspathEntry.CPE_VARIABLE:

                    addJarToList(entry.getPath(), urls);
                    break;
                case IClasspathEntry.CPE_PROJECT: {
                    if (isFirstProject || entry.isExported())
                        collectClasspathIPath(getJavaProject(entry), urls, visited, false);
                    break;
                }
            }
        }
    }

    private static void addJarToList(IPath path, List<IPath> list)
    {
        if (path.getFileExtension().equals("jar"))
        {
            list.add(path);
        }
    }

    private static URL createFileURL(IPath path)
    {
        URL url = null;
        try {
            url = new URL("file:/" + path.toOSString());

        } catch (MalformedURLException e) {
        	System.err.println(e.getMessage());
        }
        return url;
    }

    private static void collectClasspathEntryURL(IClasspathEntry entry, List<URL> urls)
    {
        URL url = createFileURL(entry.getPath());
        if (url != null)
            urls.add(url);
    }

    private static IJavaProject getJavaProject(IClasspathEntry entry)
    {
        IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(entry.getPath().segment(0));
        if (proj != null)
            return getJavaProject(proj);
        return null;
    }

    public static IJavaProject getJavaProject(IProject p)
    {
        try {
            return (IJavaProject) p.getNature(JavaCore.NATURE_ID);
        } catch (CoreException ignore) {
            return null;
        }
    }

    /**
     * Get the binary output absolute (local file system) path.
     * 
     * @param p
     *        project
     * @return project's output path or <code>null</code> if not java project or
     *         some other error.
     * 
     * @since 1.0.0
     */
    public static IPath getJavaProjectOutputAbsoluteLocation(IProject p)
    {
        IContainer container = getJavaProjectOutputContainer(p);
        if (container != null)
            return container.getLocation();
        return null;
    }

    /**
     * Get the project's binary output container.
     * 
     * @param p
     *        project
     * @return project's output container or <code>null</code> if not java project or some other error.
     * 
     * @since 1.0.0
     */
    public static IContainer getJavaProjectOutputContainer(IProject p) {
        IPath path = getJavaProjectOutputLocation(p);
        if (path == null)
            return null;
        if (path.segmentCount() == 1)
            return p;
        return p.getFolder(path.removeFirstSegments(1));
    }

    /**
     * Return the location of the binary output files for the JavaProject.
     * 
     * @param p
     *        project
     * @return path to binary output folder or <code>null</code> if not java project or other problem.
     * 
     * @since 1.0.0
     */
    public static IPath getJavaProjectOutputLocation(IProject p) {
        try {
            IJavaProject javaProj = getJavaProject(p);
            if (javaProj == null)
                return null;
            if (!javaProj.isOpen())
                javaProj.open(null);
            return javaProj.getOutputLocation();
        } catch (JavaModelException e) {
            return null;
        }
    }

    public static WorkspaceUpdater getUpdater()
    {
        return new MapWorkspaceUpdater(loaders) {
            public void update(IResource iResource, int action) {
                if (iResource != null && iResource.getName()!= null && iResource.getFileExtension()!= null && (iResource.getName().equals("pom.xml") || iResource.getFileExtension().equals("classpath")|| iResource.getFileExtension().equals("class")))
                {
                    loaders.remove(iResource.getProject().getName());
                }

            }

        };
    }

    public static IJavaProject getActiveJavaProject()
    {
        if (currentJavaProject == null)
        {
        	currentJavaProject = getActiveProjectName();          
        }

        return getJavaProject(currentJavaProject);
    }

    public static IJavaProject getJavaProject(String name)
    {
        if (name == null)
        {
            return null;
        }

        try {

            for (IJavaProject proj : getAllProjects()) {
                if (name.equals(proj.getProject().getName()))
                {
                    return proj;
                }

            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    public static String getActiveProjectName() {
        
        if (getCurrentResource() != null)
        {
            currentJavaProject = getCurrentResource().getProject().getName();              
        } 
        
    	
        return currentJavaProject;

    }

    public static void setCurrentJavaProject(String jProject) {
        if (jProject == null)
        {
        	return;
        }
        currentJavaProject = jProject;

    }

    public static IResource getCurrentResource() {
        return currentResource;
    }

    public static void setCurrentResource(IResource currentResource) {
        ClassloaderHelper.currentResource = currentResource;
    }

}