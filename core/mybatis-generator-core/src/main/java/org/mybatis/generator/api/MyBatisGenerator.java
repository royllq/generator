/**
 *    Copyright 2006-2016 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.generator.api;

import static org.mybatis.generator.internal.util.ClassloaderUtility.getCustomClassloader;
import static org.mybatis.generator.internal.util.messages.Messages.getString;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mybatis.generator.codegen.RootClassInfo;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.MergeConstants;
import org.mybatis.generator.exception.InvalidConfigurationException;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.mybatis.generator.internal.ObjectFactory;
import org.mybatis.generator.internal.NullProgressCallback;
import org.mybatis.generator.internal.XmlFileMergerJaxp;

/**
 * This class is the main interface to MyBatis generator. A typical execution of the tool involves these steps:
 * 
 * <ol>
 * <li>Create a Configuration object. The Configuration can be the result of a parsing the XML configuration file, or it
 * can be created solely in Java.</li>
 * <li>Create a MyBatisGenerator object</li>
 * <li>Call one of the generate() methods</li>
 * </ol>
 *
 * @author Jeff Butler
 * @see org.mybatis.generator.config.xml.ConfigurationParser
 */
public class MyBatisGenerator {

    /** The configuration. */
    private Configuration configuration;

    /** The shell callback. */
    private ShellCallback shellCallback;

    /** The generated java files. */
    private List<GeneratedJavaFile> generatedJavaFiles;

    /** The generated xml files. */
    private List<GeneratedXmlFile> generatedXmlFiles;

    /** The warnings. */
    private List<String> warnings;

    /** The projects. */
    private Set<String> projects;

    /**
     * Constructs a MyBatisGenerator object.
     * 
     * @param configuration
     *            The configuration for this invocation
     * @param shellCallback
     *            an instance of a ShellCallback interface. You may specify
     *            <code>null</code> in which case the DefaultShellCallback will
     *            be used.
     * @param warnings
     *            Any warnings generated during execution will be added to this
     *            list. Warnings do not affect the running of the tool, but they
     *            may affect the results. A typical warning is an unsupported
     *            data type. In that case, the column will be ignored and
     *            generation will continue. You may specify <code>null</code> if
     *            you do not want warnings returned.
     * @throws InvalidConfigurationException
     *             if the specified configuration is invalid
     */
    public MyBatisGenerator(Configuration configuration, ShellCallback shellCallback,
            List<String> warnings) throws InvalidConfigurationException {
        super();
        if (configuration == null) {
            throw new IllegalArgumentException(getString("RuntimeError.2")); //$NON-NLS-1$
        } else {
            this.configuration = configuration;
        }

        if (shellCallback == null) {
            this.shellCallback = new DefaultShellCallback(false);
        } else {
            this.shellCallback = shellCallback;
        }

        if (warnings == null) {
            this.warnings = new ArrayList<String>();
        } else {
            this.warnings = warnings;
        }
        generatedJavaFiles = new ArrayList<GeneratedJavaFile>();
        generatedXmlFiles = new ArrayList<GeneratedXmlFile>();
        projects = new HashSet<String>();

        this.configuration.validate();
    }

    /**
     * This is the main method for generating code. This method is long running, but progress can be provided and the
     * method can be canceled through the ProgressCallback interface. This version of the method runs all configured
     * contexts.
     *
     * @param callback
     *            an instance of the ProgressCallback interface, or <code>null</code> if you do not require progress
     *            information
     * @throws SQLException
     *             the SQL exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws InterruptedException
     *             if the method is canceled through the ProgressCallback
     */
    public void generate(ProgressCallback callback) throws SQLException,
            IOException, InterruptedException {
        generate(callback, null, null, true);
    }

    /**
     * This is the main method for generating code. This method is long running, but progress can be provided and the
     * method can be canceled through the ProgressCallback interface.
     *
     * @param callback
     *            an instance of the ProgressCallback interface, or <code>null</code> if you do not require progress
     *            information
     * @param contextIds
     *            a set of Strings containing context ids to run. Only the contexts with an id specified in this list
     *            will be run. If the list is null or empty, than all contexts are run.
     * @throws SQLException
     *             the SQL exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws InterruptedException
     *             if the method is canceled through the ProgressCallback
     */
    public void generate(ProgressCallback callback, Set<String> contextIds)
            throws SQLException, IOException, InterruptedException {
        generate(callback, contextIds, null, true);
    }

    /**
     * This is the main method for generating code. This method is long running, but progress can be provided and the
     * method can be cancelled through the ProgressCallback interface.
     *
     * @param callback
     *            an instance of the ProgressCallback interface, or <code>null</code> if you do not require progress
     *            information
     * @param contextIds
     *            a set of Strings containing context ids to run. Only the contexts with an id specified in this list
     *            will be run. If the list is null or empty, than all contexts are run.
     * @param fullyQualifiedTableNames
     *            a set of table names to generate. The elements of the set must be Strings that exactly match what's
     *            specified in the configuration. For example, if table name = "foo" and schema = "bar", then the fully
     *            qualified table name is "foo.bar". If the Set is null or empty, then all tables in the configuration
     *            will be used for code generation.
     * @throws SQLException
     *             the SQL exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws InterruptedException
     *             if the method is canceled through the ProgressCallback
     */
    public void generate(ProgressCallback callback, Set<String> contextIds,
            Set<String> fullyQualifiedTableNames) throws SQLException,
            IOException, InterruptedException {
        generate(callback, contextIds, fullyQualifiedTableNames, true);
    }

    /**
     * This is the main method for generating code. This method is long running, but progress can be provided and the
     * method can be cancelled through the ProgressCallback interface.
     *
     * @param callback
     *            an instance of the ProgressCallback interface, or <code>null</code> if you do not require progress
     *            information
     * @param contextIds
     *            a set of Strings containing context ids to run. Only the contexts with an id specified in this list
     *            will be run. If the list is null or empty, than all contexts are run.
     * @param fullyQualifiedTableNames
     *            a set of table names to generate. The elements of the set must be Strings that exactly match what's
     *            specified in the configuration. For example, if table name = "foo" and schema = "bar", then the fully
     *            qualified table name is "foo.bar". If the Set is null or empty, then all tables in the configuration
     *            will be used for code generation.
     * @param writeFiles
     *            if true, then the generated files will be written to disk.  If false,
     *            then the generator runs but nothing is written
     * @throws SQLException
     *             the SQL exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws InterruptedException
     *             if the method is canceled through the ProgressCallback
     */
    public void generate(ProgressCallback callback, Set<String> contextIds,
            Set<String> fullyQualifiedTableNames, boolean writeFiles) throws SQLException,
            IOException, InterruptedException {

        if (callback == null) {
            callback = new NullProgressCallback();
        }

        generatedJavaFiles.clear();
        generatedXmlFiles.clear();
        ObjectFactory.reset();
        RootClassInfo.reset();

        // calculate the contexts to run
        List<Context> contextsToRun;
        if (contextIds == null || contextIds.size() == 0) {
            contextsToRun = configuration.getContexts();
        } else {
            contextsToRun = new ArrayList<Context>();
            for (Context context : configuration.getContexts()) {
                if (contextIds.contains(context.getId())) {
                    contextsToRun.add(context);
                }
            }
        }

        // setup custom classloader if required
        if (configuration.getClassPathEntries().size() > 0) {
            ClassLoader classLoader = getCustomClassloader(configuration.getClassPathEntries());
            ObjectFactory.addExternalClassLoader(classLoader);
        }

        // now run the introspections...
        int totalSteps = 0;
        for (Context context : contextsToRun) {
            totalSteps += context.getIntrospectionSteps();
        }
        callback.introspectionStarted(totalSteps);

        for (Context context : contextsToRun) {
            context.introspectTables(callback, warnings,
                    fullyQualifiedTableNames);
        }

        // now run the generates
        totalSteps = 0;
        for (Context context : contextsToRun) {
            totalSteps += context.getGenerationSteps();
        }
        callback.generationStarted(totalSteps);

        for (Context context : contextsToRun) {
            context.generateFiles(callback, generatedJavaFiles,
                    generatedXmlFiles, warnings);
        }

        // now save the files
        if (writeFiles) {
            callback.saveStarted(generatedXmlFiles.size()
                + generatedJavaFiles.size());

            for (GeneratedXmlFile gxf : generatedXmlFiles) {
                projects.add(gxf.getTargetProject());
                writeGeneratedXmlFile(gxf, callback);
            }

            for (GeneratedJavaFile gjf : generatedJavaFiles) {
                projects.add(gjf.getTargetProject());
                writeGeneratedJavaFile(gjf, callback);
            }

            for (String project : projects) {
                shellCallback.refreshProject(project);
            }
        }

        callback.done();
    }

    private void writeGeneratedJavaFile(GeneratedJavaFile gjf, ProgressCallback callback)
            throws InterruptedException, IOException {
        File targetFile;
        String source;
        try {
            File directory = shellCallback.getDirectory(gjf
                    .getTargetProject(), gjf.getTargetPackage());
            targetFile = new File(directory, gjf.getFileName());
            if (targetFile.exists()) {
                if (shellCallback.isMergeSupported()) {
                    source = shellCallback.mergeJavaFile(gjf
                            .getFormattedContent(), targetFile
                            .getAbsolutePath(),
                            MergeConstants.OLD_ELEMENT_TAGS,
                            gjf.getFileEncoding());
                } else if (shellCallback.isOverwriteEnabled()) {
                    source = gjf.getFormattedContent();
                    warnings.add(getString("Warning.11", //$NON-NLS-1$
                            targetFile.getAbsolutePath()));
                } else {
                    source = gjf.getFormattedContent();
                    targetFile = getUniqueFileName(directory, gjf
                            .getFileName());
                    warnings.add(getString(
                            "Warning.2", targetFile.getAbsolutePath())); //$NON-NLS-1$
                }
            } else {
                source = gjf.getFormattedContent();
            }

            callback.checkCancel();
            callback.startTask(getString(
                    "Progress.15", targetFile.getName())); //$NON-NLS-1$
//            writeFile(targetFile, source, gjf.getFileEncoding());
            
            
            String modelName = targetFile.getName().replace(".java", "");
            modelName = modelName.replace("Mapper", "");
            modelName = modelName.replace("Example", "");

            String modelNameEss = new StringBuilder().append(modelName).append("Ess").toString();

            String parentPackage = gjf.getTargetPackage().substring(0, gjf.getTargetPackage().lastIndexOf("."));
            parentPackage = parentPackage.substring(0, parentPackage.lastIndexOf("."));

            if (targetFile.getName().endsWith("Mapper.java"))
            {
              source = source.replace(modelName, modelNameEss);
              source = source.replace(new StringBuilder().append(modelNameEss).append("Example").toString(), new StringBuilder().append(modelName).append("Example").toString());
              source = source.replace(new StringBuilder().append(modelNameEss).append("Mapper").toString(), new StringBuilder().append(modelName).append("MapperEss").toString());

              GeneratedJavaFile modelJavaFile = findJavaFile(modelName);
              source = source.replace(new StringBuilder().append(modelJavaFile.getTargetPackage()).append(".").append(modelName).append("Ess").toString(), new StringBuilder().append(parentPackage).append(".model.").append(modelName).toString());
              source = source.replace(new StringBuilder().append(modelName).append("Ess").toString(), modelName);

              File f = new File(new StringBuilder().append(targetFile.getParent()).append("/").append(modelName).append("MapperEss.java").toString());
              writeFile(f, source, gjf.getFileEncoding());

              String repName = "";

              File serviceFile = new File(new StringBuilder().append(new StringBuilder().append(gjf.getTargetProject()).append(".").append(parentPackage).append(".service.").toString().replace(".", "/")).append("/").append(modelName).append("Service.java").toString());
              repName = serviceFile.getName().replace("Service.java", "");
              repName = new StringBuilder().append(repName.substring(0, 1).toLowerCase()).append(repName.substring(1)).toString();
              if (!serviceFile.exists()) {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append(new StringBuilder().append("package ").append(parentPackage).append(".service;\n").toString());
                sbuf.append("\n");
                sbuf.append(new StringBuilder().append("import ").append(parentPackage).append(".dao.").append(modelName).append("Mapper;\n").toString());
                sbuf.append("import org.springframework.stereotype.Service;\n");
                sbuf.append("import org.springframework.beans.factory.annotation.Autowired;\n");
                sbuf.append("\n");
                sbuf.append(new StringBuilder().append("@Service(\"").append(repName).append("Service\")\n").toString());
                sbuf.append(new StringBuilder().append("public class ").append(modelName).append("Service {\n").toString());
                sbuf.append("\t@Autowired \n");
                sbuf.append(new StringBuilder().append("\tprivate ").append(modelName).append("Mapper ").append(repName).append("Mapper; \n").toString());
                sbuf.append("}\n");
                writeFile(serviceFile, sbuf.toString(), gjf.getFileEncoding());
              } else {
                System.out.println(new StringBuilder().append(serviceFile.getAbsolutePath()).append(" exists!").toString());
              }

              File mapperFile = new File(new StringBuilder().append(new StringBuilder().append(gjf.getTargetProject()).append(".").append(parentPackage).append(".dao.").toString().replace(".", "/")).append("/").append(modelName).append("Mapper.java").toString());
              repName = mapperFile.getName().replace("Mapper.java", "");
              repName = new StringBuilder().append(repName.substring(0, 1).toLowerCase()).append(repName.substring(1)).toString();
              if (!mapperFile.exists()) {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append(new StringBuilder().append("package ").append(parentPackage).append(".dao;\n").toString());
                sbuf.append("\n");
                sbuf.append(new StringBuilder().append("import ").append(findJavaFile(new StringBuilder().append(modelName).append("Mapper").toString()).getTargetPackage()).append(".").append(modelName).append("MapperEss;\n").toString());
                sbuf.append("import org.springframework.stereotype.Repository;\n");
                sbuf.append("\n");
                sbuf.append(new StringBuilder().append("@Repository(\"").append(repName).append("Mapper\")\n").toString());
                sbuf.append(new StringBuilder().append("public interface ").append(modelName).append("Mapper extends ").append(modelName).append("MapperEss {\n").toString());
                sbuf.append("}\n");
                writeFile(mapperFile, sbuf.toString(), gjf.getFileEncoding());
              } else {
                System.out.println(new StringBuilder().append(mapperFile.getAbsolutePath()).append(" exists!").toString());
              }
            } else if (targetFile.getName().endsWith("Example.java")) {
              File f = new File(new StringBuilder().append(targetFile.getParent()).append("/").append(modelName).append("Example.java").toString());
              writeFile(f, source, gjf.getFileEncoding());
            }
            else {
              source = source.replace(new StringBuilder().append("public class ").append(modelName).append(" {").toString(), new StringBuilder().append("public class ").append(modelNameEss).append(" {").toString());
              File f = new File(new StringBuilder().append(targetFile.getParent()).append("/").append(modelNameEss).append(".java").toString());
              writeFile(f, source, gjf.getFileEncoding());

              File modelFile = new File(new StringBuilder().append(new StringBuilder().append(gjf.getTargetProject()).append(".").append(parentPackage).append(".model.").toString().replace(".", "/")).append("/").append(modelName).append(".java").toString());
              String repName = modelFile.getName().replace(".java", "");
              repName = new StringBuilder().append(repName.substring(0, 1).toLowerCase()).append(repName.substring(1)).toString();
              if (!modelFile.exists()) {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append(new StringBuilder().append("package ").append(parentPackage).append(".model;\n").toString());
                sbuf.append("\n");
                sbuf.append(new StringBuilder().append("import ").append(findJavaFile(modelName).getTargetPackage()).append(".").append(modelName).append("Ess;\n").toString());
                sbuf.append("\n");
                sbuf.append(new StringBuilder().append("public class ").append(modelName).append(" extends ").append(modelName).append("Ess {\n").toString());
                sbuf.append("}\n");
                writeFile(modelFile, sbuf.toString(), gjf.getFileEncoding());
              } else {
                System.out.println(new StringBuilder().append(modelFile.getAbsolutePath()).append(" exists!").toString());
              }
            }
            
            
        } catch (ShellException e) {
            warnings.add(e.getMessage());
        }
    }

    private void writeGeneratedXmlFile(GeneratedXmlFile gxf, ProgressCallback callback)
            throws InterruptedException, IOException {
        File targetFile;
        String source;
        try {
            File directory = shellCallback.getDirectory(gxf
                    .getTargetProject(), gxf.getTargetPackage());
            targetFile = new File(directory, gxf.getFileName());
            if (targetFile.exists()) {
                if (gxf.isMergeable()) {
                    source = XmlFileMergerJaxp.getMergedSource(gxf,
                            targetFile);
                } else if (shellCallback.isOverwriteEnabled()) {
                    source = gxf.getFormattedContent();
                    warnings.add(getString("Warning.11", //$NON-NLS-1$
                            targetFile.getAbsolutePath()));
                } else {
                    source = gxf.getFormattedContent();
                    targetFile = getUniqueFileName(directory, gxf
                            .getFileName());
                    warnings.add(getString(
                            "Warning.2", targetFile.getAbsolutePath())); //$NON-NLS-1$
                }
            } else {
                source = gxf.getFormattedContent();
            }

            callback.checkCancel();
            callback.startTask(getString(
                    "Progress.15", targetFile.getName())); //$NON-NLS-1$
//            writeFile(targetFile, source, "UTF-8"); //$NON-NLS-1$
            
            if (targetFile.getName().endsWith("Mapper.xml"))
            {
              String modelName = targetFile.getName().replace(".xml", "");
              modelName = modelName.replace("Mapper", "");

              String mapperName = targetFile.getName().replace(".xml", "");

              GeneratedJavaFile mapperJavaFile = findJavaFile(mapperName);
              GeneratedJavaFile modelJavaFile = findJavaFile(modelName);

              String parentPackage = modelJavaFile.getTargetPackage().substring(0, modelJavaFile.getTargetPackage().lastIndexOf("."));
              parentPackage = parentPackage.substring(0, parentPackage.lastIndexOf("."));

              if (mapperJavaFile != null)
              {
                source = source.replace(new StringBuilder().append(mapperJavaFile.getTargetPackage()).append(".").append(mapperName).toString(), new StringBuilder().append(parentPackage).append(".dao.").append(mapperName).toString());
                if (modelJavaFile != null) {
                  source = source.replace(new StringBuilder().append(modelJavaFile.getTargetPackage()).append(".").append(modelName).toString(), new StringBuilder().append(parentPackage).append(".model.").append(modelName).toString());
                  source = source.replace(new StringBuilder().append(parentPackage).append(".model.").append(modelName).append("Example").toString(), new StringBuilder().append(modelJavaFile.getTargetPackage()).append(".").append(modelName).append("Example").toString());
                }
                writeFile(targetFile, source, "UTF-8");

                String parentXmlPackage = gxf.getTargetPackage();
                int idx = parentXmlPackage.lastIndexOf("/");
                if (idx > 0) {
                  parentXmlPackage = parentXmlPackage.substring(0, parentXmlPackage.lastIndexOf("/"));
                }

                idx = parentXmlPackage.lastIndexOf("\\");
                if (idx > 0) {
                  parentXmlPackage = parentXmlPackage.substring(0, parentXmlPackage.lastIndexOf("\\"));
                }

                parentXmlPackage = new StringBuilder().append(gxf.getTargetProject()).append("/").append(parentXmlPackage).toString();

                File _targetFile = new File(new StringBuilder().append(parentXmlPackage).append("/").append(mapperName).append(".xml").toString());
                if (!_targetFile.exists()) {
                  StringBuffer sbuf = new StringBuffer();
                  sbuf.append("<?xml version=\"1.0\" encoding=\"GBK\" ?>\n");
                  sbuf.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >\n");
                  sbuf.append(new StringBuilder().append("<mapper namespace=\"").append(parentPackage).append(".dao.").append(mapperName).append("\" >\n").toString());
                  sbuf.append("\n");
                  sbuf.append("</mapper>\n");
                  writeFile(_targetFile, sbuf.toString(), "UTF-8");
                } else {
                  System.out.println(new StringBuilder().append(_targetFile.getAbsolutePath()).append(" exists!").toString());
                }
              } else {
                writeFile(targetFile, source, "UTF-8");
              }
            }
            
        } catch (ShellException e) {
            warnings.add(e.getMessage());
        }
    }
    
    private GeneratedJavaFile findJavaFile(String javaName) {
        GeneratedJavaFile targetFile = null;
        for (GeneratedJavaFile _gjf : this.generatedJavaFiles) {
          if (_gjf.getFileName().replace(".java", "").equals(javaName)) {
            targetFile = _gjf;
            break;
          }
        }
        return targetFile;
      }

    /**
     * Writes, or overwrites, the contents of the specified file.
     *
     * @param file
     *            the file
     * @param content
     *            the content
     * @param fileEncoding
     *            the file encoding
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void writeFile(File file, String content, String fileEncoding) throws IOException {
        FileOutputStream fos = new FileOutputStream(file, false);
        OutputStreamWriter osw;
        if (fileEncoding == null) {
            osw = new OutputStreamWriter(fos);
        } else {
            osw = new OutputStreamWriter(fos, fileEncoding);
        }
        
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(content);
        bw.close();
    }

    /**
     * Gets the unique file name.
     *
     * @param directory
     *            the directory
     * @param fileName
     *            the file name
     * @return the unique file name
     */
    private File getUniqueFileName(File directory, String fileName) {
        File answer = null;

        // try up to 1000 times to generate a unique file name
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 1000; i++) {
            sb.setLength(0);
            sb.append(fileName);
            sb.append('.');
            sb.append(i);

            File testFile = new File(directory, sb.toString());
            if (!testFile.exists()) {
                answer = testFile;
                break;
            }
        }

        if (answer == null) {
            throw new RuntimeException(getString(
                    "RuntimeError.3", directory.getAbsolutePath())); //$NON-NLS-1$
        }

        return answer;
    }

    /**
     * Returns the list of generated Java files after a call to one of the generate methods.
     * This is useful if you prefer to process the generated files yourself and do not want
     * the generator to write them to disk.
     *  
     * @return the list of generated Java files
     */
    public List<GeneratedJavaFile> getGeneratedJavaFiles() {
        return generatedJavaFiles;
    }

    /**
     * Returns the list of generated XML files after a call to one of the generate methods.
     * This is useful if you prefer to process the generated files yourself and do not want
     * the generator to write them to disk.
     *  
     * @return the list of generated XML files
     */
    public List<GeneratedXmlFile> getGeneratedXmlFiles() {
        return generatedXmlFiles;
    }
}
