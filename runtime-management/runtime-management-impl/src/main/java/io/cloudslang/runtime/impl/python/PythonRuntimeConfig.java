package io.cloudslang.runtime.impl.python;

import org.python.util.PythonInterpreter;
import org.python.core.Options;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PythonRuntimeConfig {
    static {
        Options.importSite = false;
    }

    @Bean
    public PythonInterpreter evalInterpreter(){
        return new PythonInterpreter();
    }

    @Bean
    public PythonInterpreter execInterpreter(){
        PythonInterpreter interpreter = new PythonInterpreter();
        //here to avoid jython preferring io.cloudslang package over python io package
        interpreter.exec("import io");
        return interpreter;
    }
}
