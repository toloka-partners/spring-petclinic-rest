/*
 * Copyright 2002-2024 the original author or authors.
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
package org.springframework.samples.petclinic.util;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

/**
 * SQL Query Count Interceptor to track the number of SQL queries executed.
 * This is used to measure N+1 query problems and their fixes.
 */
@Component
public class SqlQueryCountInterceptor implements StatementInspector {
    
    private static final ThreadLocal<Integer> queryCount = new ThreadLocal<>();
    
    @Override
    public String inspect(String sql) {
        Integer count = queryCount.get();
        if (count == null) {
            count = 0;
        }
        queryCount.set(count + 1);
        return sql;
    }
    
    public static void startCounting() {
        queryCount.set(0);
    }
    
    public static int getQueryCount() {
        Integer count = queryCount.get();
        return count != null ? count : 0;
    }
    
    public static void reset() {
        queryCount.remove();
    }
}