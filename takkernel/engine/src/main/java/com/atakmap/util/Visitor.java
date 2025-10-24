package com.atakmap.util;

import gov.tak.api.annotation.DontObfuscate;
@DontObfuscate
public interface Visitor<T>
{
    void visit(T object);
}
