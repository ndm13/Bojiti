package net.miscfolder.bojiti.downloader;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Protocols{
	String[] value();
}
