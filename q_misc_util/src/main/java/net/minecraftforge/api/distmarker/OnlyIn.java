package net.minecraftforge.api.distmarker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A fake dependency for making the developing the Forge version of Immersive Portals mod easier.
 * It's kept the same as Forge. Except that the Retention is changed to SOURCE,
 * so it will not be in the compiled class file.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
public @interface OnlyIn
{
    public Dist value();
    public Class<?> _interface() default Object.class;
}
