Feature: Classrooms directory

    In order to look up registered classrooms
    As Lyra
    I want to list and retrieve classroom records

    Background:
        Given a school named "Gloria Fuertes" exists
        And the following classrooms exist at school "Gloria Fuertes":
            | course | group |
            | 1      | A     |
            | 2      | B     |
            | 3      | C     |
            | 4      | D     |
            | 5      | E     |
        And I am authenticated with "classrooms.read" scope

    Scenario: List classrooms
        When I request the list of classrooms
        Then the list of classrooms contains exactly the following classrooms:
            | course | group |
            | 1      | A     |
            | 2      | B     |
            | 3      | C     |
            | 4      | D     |
            | 5      | E     |

    Scenario: List classrooms with pagination
        When I request the list of classrooms with page size 2 and page number 0
        Then I receive a page of 2 classrooms out of a total of 5

    Scenario: List classrooms with pagination, second page
        When I request the list of classrooms with page size 2 and page number 1
        Then I receive a page of 2 classrooms out of a total of 5

    Scenario: Get a classroom
        When I request classroom for course 1 group "A"
        Then I receive the details of classroom for course 1 group "A"

    Scenario: Cannot get a classroom that does not exist
        When I request a classroom that does not exist
        Then I receive a not found error
