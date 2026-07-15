Feature: Classrooms directory

    In order to look up registered classrooms
    As Lyra
    I want to list and retrieve classroom records

    Background:
        Given a school named "Gloria Fuertes" exists
        And a classroom for course 1 group "A" exists at school "Gloria Fuertes"
        And a classroom for course 2 group "B" exists at school "Gloria Fuertes"
        And a classroom for course 3 group "C" exists at school "Gloria Fuertes"
        And I am authenticated with "classrooms.read" scope

    Scenario: List classrooms
        When I request the list of classrooms
        Then the list of classrooms includes course 1 group "A"

    Scenario: List classrooms with pagination
        When I request the list of classrooms with page size 2 and page number 0
        Then I receive a page of 2 classrooms out of a total of 3

    Scenario: List classrooms with pagination, second page
        When I request the list of classrooms with page size 2 and page number 1
        Then I receive a page of 1 classrooms out of a total of 3

    Scenario: Get a classroom
        When I request classroom for course 1 group "A"
        Then I receive the details of classroom for course 1 group "A"

    Scenario: Cannot get a classroom that does not exist
        When I request a classroom that does not exist
        Then I receive a not found error
