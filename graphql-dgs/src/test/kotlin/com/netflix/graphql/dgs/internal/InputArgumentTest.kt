/*
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.graphql.dgs.internal

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.netflix.graphql.dgs.scalars.UploadScalar
import graphql.ExceptionWhileDataFetching
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.schema.DataFetchingEnvironment
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Suppress("unused")
@ExtendWith(MockKExtension::class)
internal class InputArgumentTest {
    @MockK
    lateinit var applicationContextMock: ApplicationContext

    @Test
    fun `@InputArgument with name specified on String argument`() {
        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@InputArgument("name") abc: String): String {
                return "Hello, $abc"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())
        val schema = provider.schema()

        val build = GraphQL.newGraphQL(schema).build()
        val executionResult = build.execute("""{hello(name: "tester")}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, tester", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `@InputArgument with no name specified should work`() {
        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@InputArgument name: String): String {
                return "Hello, $name"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())
        val schema = provider.schema()

        val build = GraphQL.newGraphQL(schema).build()
        val executionResult = build.execute("""{hello(name: "tester")}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, tester", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `@InputArgument with no name specified, without matching argument, should be null`() {
        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@InputArgument abc: String?): String {
                return "Hello, ${abc ?: "no name"}"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())
        val schema = provider.schema()

        val build = GraphQL.newGraphQL(schema).build()
        val executionResult = build.execute("""{hello(name: "tester")}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, no name", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `@InputArgument on an input type`() {
        val schema = """
            type Query {
                hello(person:Person): String
            }
            
            input Person {
                name:String
            }
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@InputArgument("person") person: Person): String {
                return "Hello, ${person.name}"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""{hello(person: {name: "tester"})}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, tester", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `@InputArgument with name specified as 'name'`() {
        val schema = """
            type Query {
                hello(person:Person): String
            }
            
            input Person {
                name:String
            }
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@InputArgument(name = "person") person: Person): String {
                return "Hello, ${person.name}"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""{hello(person: {name: "tester"})}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, tester", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `@InputArgument on a list of strings`() {
        val schema = """
            type Query {
                hello(names: [String]): String
            }
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@InputArgument("names") names: List<String>): String {
                return "Hello, ${names.joinToString(", ")}"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""{hello(names: ["tester 1", "tester 2"])}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, tester 1, tester 2", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `@InputArgument on a list of input types`() {
        val schema = """
            type Query {
                hello(person:[Person]): String
            }
            
            input Person {
                name:String
            }
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@InputArgument("person", collectionType = Person::class) person: List<Person>): String {
                return "Hello, ${person.joinToString(", ") { it.name }}"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""{hello(person: [{name: "tester"}, {name: "tester 2"}])}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, tester, tester 2", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `Input argument of type Set should result in a DgsInvalidInputArgumentException`() {
        val schema = """
            type Query {
                hello(person:[Person]): String
            }
            
            input Person {
                name:String
            }
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@InputArgument("person", collectionType = Person::class) person: Set<Person>): String {
                return "Hello, ${person.joinToString(", ") { it.name }}"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()

        val executionResult = build.execute("""{hello(person: [{name: "tester"}, {name: "tester 2"}])}""")
        org.assertj.core.api.Assertions.assertThat(executionResult.errors.size).isEqualTo(1)
        val exceptionWhileDataFetching = executionResult.errors[0] as ExceptionWhileDataFetching
        org.assertj.core.api.Assertions.assertThat(exceptionWhileDataFetching.exception).isInstanceOf(DgsInvalidInputArgumentException::class.java)
        org.assertj.core.api.Assertions.assertThat(exceptionWhileDataFetching.exception.message).contains("Specified type 'interface java.util.Set' is invalid. Found java.util.ArrayList instead")

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `A DgsInvalidInputArgumentException should be thrown when the @InputArgument collectionType doesn't match the parameter type`() {
        val schema = """
            type Query {
                hello(person:[Person]): String
            }
            
            input Person {
                name:String
            }
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@InputArgument("person", collectionType = String::class) person: List<String>): String {
                return "Hello, ${person.joinToString(", ") { it }}"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()

        val executionResult = build.execute("""{hello(person: [{name: "tester"}, {name: "tester 2"}])}""")
        org.assertj.core.api.Assertions.assertThat(executionResult.errors.size).isEqualTo(1)
        val exceptionWhileDataFetching = executionResult.errors[0] as ExceptionWhileDataFetching
        org.assertj.core.api.Assertions.assertThat(exceptionWhileDataFetching.exception).isInstanceOf(DgsInvalidInputArgumentException::class.java)
        org.assertj.core.api.Assertions.assertThat(exceptionWhileDataFetching.exception.message).contains("Specified type 'class java.lang.String' is invalid for person")

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `A null value for an input argument should not result in an error`() {
        val schema = """
            type Query {
                hello(person:Person): String
            }
            
            input Person {
                name:String
            }
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@InputArgument("person") person: Person?): String {
                if (person == null) {
                    return "Hello, Stranger"
                }

                return "Hello, ${person.name}"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""{hello}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, Stranger", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `Multiple input arguments should be supported`() {
        val schema = """
            type Query {
                hello(person:Person, capitalize:Boolean): String
            }
            
            input Person {
                name:String
            }
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(
                @InputArgument("capitalize") capitalize: Boolean,
                @InputArgument("person") person: Person
            ): String {
                return if (capitalize) {
                    "hello, ${person.name}".capitalize()
                } else {
                    "hello, ${person.name}"
                }
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""{hello(capitalize: true, person: {name: "tester"})}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, tester", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `Multiple input argument in combination with a DataFetchingEnvironment argument should be supported`() {
        val schema = """
            type Query {
                hello(person:Person, capitalize:Boolean, otherArg:String): String
            }
            
            input Person {
                name:String
            }
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(
                dfe: DataFetchingEnvironment,
                @InputArgument("capitalize") capitalize: Boolean,
                @InputArgument("person") person: Person
            ): String {
                val otherArg: String = dfe.getArgument("otherArg")

                val msg = if (capitalize) {
                    "hello, ${person.name}".capitalize()
                } else {
                    "hello, ${person.name}"
                }
                return msg + otherArg
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""{hello(capitalize: true, person: {name: "tester"}, otherArg: "!")}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, tester!", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `Multiple input argument in combination with a DataFetchingEnvironment argument should be supported in any order`() {
        val schema = """
            type Query {
                hello(person:Person, capitalize:Boolean, otherArg:String): String
            }
            
            input Person {
                name:String
            }
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(
                @InputArgument("capitalize") capitalize: Boolean,
                @InputArgument("person") person: Person,
                dfe: DataFetchingEnvironment
            ): String {
                val otherArg: String = dfe.getArgument("otherArg")

                val msg = if (capitalize) {
                    "hello, ${person.name}".capitalize()
                } else {
                    "hello, ${person.name}"
                }
                return msg + otherArg
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""{hello(capitalize: true, person: {name: "tester"}, otherArg: "!")}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, tester!", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `Input arguments of type MultipartFile should be supported`() {
        val schema = """
            type Mutation {
                upload(file: Upload): String
            }

            scalar Upload
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Mutation", field = "upload")
            fun someFetcher(@InputArgument("file") file: MultipartFile): String {
                return String(file.bytes)
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            ),
            Pair("Upload", UploadScalar()),
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val file: MultipartFile =
            MockMultipartFile("hello.txt", "hello.txt", MediaType.TEXT_PLAIN_VALUE, "Hello World".toByteArray())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute(
            ExecutionInput.newExecutionInput().query("mutation(\$input: Upload!)  { upload(file: \$input) }")
                .variables(mapOf(Pair("input", file)))
        )
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello World", data["upload"])
    }

    @Test
    fun `An unknown argument should be null, and not result in an error`() {
        val schema = """
            type Query {
                hello: String
            }
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(someArg: String?): String {

                return "Hello, $someArg"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""{hello}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("Hello, null", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `Scalars should be supported as input arguments - testing with DateTime`() {
        val schema = """
            type Mutation {
                setDate(date:DateTime): String
            }                     
            
            scalar DateTime
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Mutation", field = "setDate")
            fun someFetcher(@InputArgument("date") date: LocalDateTime): String {
                return "The date is: ${date.format(DateTimeFormatter.ISO_DATE)}"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns mapOf(Pair("DateTime", LocalDateTimeScalar()))

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""mutation {setDate(date: "2021-01-27T10:15:30")}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("The date is: 2021-01-27", data["setDate"])
    }

    @Test
    fun `Lists of scalars should be supported - testing with list of DateTime`() {
        val schema = """
            type Mutation {
                setDate(date:[DateTime]): String
            }                     
            
            scalar DateTime
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Mutation", field = "setDate")
            fun someFetcher(@InputArgument("date") date: List<LocalDateTime>): String {
                return "The date is: ${date[0].format(DateTimeFormatter.ISO_DATE)}"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns mapOf(Pair("DateTime", LocalDateTimeScalar()))

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""mutation {setDate(date: ["2021-01-27T10:15:30"])}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("The date is: 2021-01-27", data["setDate"])
    }

    @Test
    fun `Scalars should work even when nested in an input type`() {
        val schema = """
            type Mutation {
                setDate(input:DateTimeInput): String
            }
            
            input DateTimeInput {
                date: DateTime
            }
            
            scalar DateTime
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Mutation", field = "setDate")
            fun someFetcher(@InputArgument("input") input: DateTimeInput): String {
                return "The date is: ${input.date.format(DateTimeFormatter.ISO_DATE)}"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns mapOf(Pair("DateTime", LocalDateTimeScalar()))

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""mutation {setDate(input: {date: "2021-01-27T10:15:30"})}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("The date is: 2021-01-27", data["setDate"])
    }

    @Test
    fun `Lists of primitive types should be supported`() {
        val schema = """
            type Mutation {
                setRatings(ratings:[Int!]): [Int]
            }                     
            
        """.trimIndent()

        val fetcher = object : Any() {
            @DgsData(parentType = "Mutation", field = "setRatings")
            @Suppress("UNUSED_PARAMETER")
            fun someFetcher(@InputArgument("ratings") ratings: List<Int>): List<Int> {
                return listOf(1, 2, 3)
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())

        val build = GraphQL.newGraphQL(provider.schema(schema)).build()
        val executionResult = build.execute("""mutation {setRatings(ratings: [1, 2, 3])}""")
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals(listOf(1, 2, 3), data["setRatings"])
    }

    @Test
    fun `A @RequestHeader argument without name should be supported`() {
        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@RequestHeader referer: String): String {
                return "From, $referer"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())
        val schema = provider.schema()

        val build = GraphQL.newGraphQL(schema).build()
        val httpHeaders = HttpHeaders()
        httpHeaders.add("Referer", "localhost")
        val executionResult = build.execute(ExecutionInput.newExecutionInput("""{hello}""").context(DgsContext(null, DgsRequestData(emptyMap(), httpHeaders))))
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("From, localhost", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `A @RequestHeader argument with name should be supported`() {
        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@RequestHeader("referer") input: String): String {
                return "From, $input"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())
        val schema = provider.schema()

        val build = GraphQL.newGraphQL(schema).build()
        val httpHeaders = HttpHeaders()
        httpHeaders.add("Referer", "localhost")
        val executionResult = build.execute(
            ExecutionInput.newExecutionInput("""{hello}""")
                .context(DgsContext(null, DgsRequestData(emptyMap(), httpHeaders)))
        )
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("From, localhost", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `A @RequestHeader argument with name specified in 'name' argument should be supported`() {
        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@RequestHeader(name = "referer") input: String): String {
                return "From, $input"
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())
        val schema = provider.schema()

        val build = GraphQL.newGraphQL(schema).build()
        val httpHeaders = HttpHeaders()
        httpHeaders.add("Referer", "localhost")
        val executionResult = build.execute(
            ExecutionInput.newExecutionInput("""{hello}""")
                .context(DgsContext(null, DgsRequestData(emptyMap(), httpHeaders)))
        )
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("From, localhost", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `A @RequestParam argument with name specified in 'name' argument should be supported`() {
        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@RequestParam(name = "message") input: String): String {
                return input
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())
        val schema = provider.schema()

        val build = GraphQL.newGraphQL(schema).build()

        val webRequest = mockk<WebRequest>()
        every { webRequest.parameterMap } returns mapOf("message" to listOf("My param").toTypedArray())

        val executionResult = build.execute(
            ExecutionInput.newExecutionInput("""{hello}""")
                .context(DgsContext(null, DgsRequestData(emptyMap(), null, webRequest)))
        )
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("My param", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `A @RequestParam argument with no name specified should be supported`() {
        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@RequestParam message: String): String {
                return message
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())
        val schema = provider.schema()

        val build = GraphQL.newGraphQL(schema).build()

        val webRequest = mockk<WebRequest>()
        every { webRequest.parameterMap } returns mapOf("message" to listOf("My param").toTypedArray())

        val executionResult = build.execute(
            ExecutionInput.newExecutionInput("""{hello}""")
                .context(DgsContext(null, DgsRequestData(emptyMap(), null, webRequest)))
        )
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("My param", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }

    @Test
    fun `A @RequestParam argument with no name specified as value should be supported`() {
        val fetcher = object : Any() {
            @DgsData(parentType = "Query", field = "hello")
            fun someFetcher(@RequestParam("message") input: String): String {
                return input
            }
        }

        every { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) } returns mapOf(
            Pair(
                "helloFetcher",
                fetcher
            )
        )
        every { applicationContextMock.getBeansWithAnnotation(DgsScalar::class.java) } returns emptyMap()

        val provider = DgsSchemaProvider(applicationContextMock, Optional.empty(), Optional.empty(), Optional.empty())
        val schema = provider.schema()

        val build = GraphQL.newGraphQL(schema).build()

        val webRequest = mockk<WebRequest>()
        every { webRequest.parameterMap } returns mapOf("message" to listOf("My param").toTypedArray())

        val executionResult = build.execute(
            ExecutionInput.newExecutionInput("""{hello}""")
                .context(DgsContext(null, DgsRequestData(emptyMap(), null, webRequest)))
        )
        Assertions.assertTrue(executionResult.isDataPresent)
        val data = executionResult.getData<Map<String, *>>()
        Assertions.assertEquals("My param", data["hello"])

        verify { applicationContextMock.getBeansWithAnnotation(DgsComponent::class.java) }
    }
}
