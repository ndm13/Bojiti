package net.miscfolder.bojiti.parser;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface MimeTypes{
	String[] value();
}
