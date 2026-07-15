Feature: Schools directory

    In order to look up registered schools
    As Lyra
    I want to list and retrieve school records

    Background:
        Given the following schools exist:
            | name             |
            | Gloria Fuertes   |
            | Montessori Norte |
            | San Ignacio      |
            | Santa Cecilia    |
            | El Pilar         |
        And I am authenticated with "schools.read" scope

    Scenario: List schools
        When I request the list of schools
        Then the list of schools includes "Gloria Fuertes"

    Scenario: List schools with pagination
        When I request the list of schools with page size 2 and page number 0
        Then I receive a page of 2 schools out of a total of 5

    Scenario: List schools with pagination, second page
        When I request the list of schools with page size 2 and page number 1
        Then I receive a page of 2 schools out of a total of 5

    Scenario: Get a school
        When I request school "Gloria Fuertes"
        Then I receive the details of school "Gloria Fuertes"

    Scenario: Cannot get a school that does not exist
        When I request a school that does not exist
        Then I receive a not found error
