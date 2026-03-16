package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.PropertyDataFetcher;
import graphql.schema.SingletonPropertyDataFetcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.GraphQlSource;
import org.stapledon.AbstractHttpGraphQlIntegrationTest;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * Verifies that every field in the Query and Mutation root types has a registered
 * resolver (DataFetcher) rather than the default PropertyDataFetcher.
 *
 * Spring for GraphQL silently falls back to PropertyDataFetcher for unmapped fields,
 * which returns null at runtime instead of failing at startup. This test catches
 * schema fields that lack a corresponding @QueryMapping or @MutationMapping method.
 */
@Slf4j
class GraphqlSchemaCompletenessIT extends AbstractHttpGraphQlIntegrationTest {

	@Autowired
	private GraphQlSource graphQlSource;

	@Test
	void allQueryFieldsHaveResolvers() {
		GraphQLSchema schema = graphQlSource.schema();

		GraphQLObjectType queryType = schema.getQueryType();
		assertThat(queryType).as("Schema must define a Query type").isNotNull();

		List<String> unmappedFields = findUnmappedFields(schema, queryType);

		assertThat(unmappedFields)
				.as("All Query fields must have resolvers (not PropertyDataFetcher). Unmapped: %s", unmappedFields)
				.isEmpty();
	}

	@Test
	void allMutationFieldsHaveResolvers() {
		GraphQLSchema schema = graphQlSource.schema();

		GraphQLObjectType mutationType = schema.getMutationType();
		assertThat(mutationType).as("Schema must define a Mutation type").isNotNull();

		List<String> unmappedFields = findUnmappedFields(schema, mutationType);

		assertThat(unmappedFields)
				.as("All Mutation fields must have resolvers (not PropertyDataFetcher). Unmapped: %s", unmappedFields)
				.isEmpty();
	}

	private List<String> findUnmappedFields(GraphQLSchema schema, GraphQLObjectType rootType) {
		return rootType.getFieldDefinitions().stream()
				.filter(field -> isPropertyDataFetcher(schema, rootType, field))
				.map(GraphQLFieldDefinition::getName)
				.sorted()
				.collect(Collectors.toList());
	}

	private boolean isPropertyDataFetcher(GraphQLSchema schema, GraphQLObjectType parentType, GraphQLFieldDefinition field) {
		var fetcher = schema.getCodeRegistry().getDataFetcher(parentType, field);
		log.debug("Field {}.{} -> {}", parentType.getName(), field.getName(), fetcher.getClass().getSimpleName());
		return fetcher instanceof PropertyDataFetcher || fetcher instanceof SingletonPropertyDataFetcher;
	}
}
