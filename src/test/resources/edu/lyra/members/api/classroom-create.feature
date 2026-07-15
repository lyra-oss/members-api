Feature: Classrooms' creation

    In order to organize a school's students
    As Lyra
    I want to provide creation services for classrooms

    Background:
        Given a school named "Gloria Fuertes" exists
        And I am authenticated with "classrooms.create" scope

    Scenario: Create classroom
        When I create a classroom for course 1 group "A" at school "Gloria Fuertes"
        Then I receive a confirmation that the classroom has been successfully created

    Scenario Outline: Cannot create classroom when the course is <course>
        When I create a classroom for course <course> group "A" at school "Gloria Fuertes"
        Then I receive an error stating that "course" field is incorrect because "must be greater than 0"

        Examples:
            | course |
            | 0      |
            | -1     |

    Scenario: Cannot create classroom when the course is greater than 6
        When I create a classroom for course 7 group "A" at school "Gloria Fuertes"
        Then I receive an error stating that "course" field is incorrect because "must be less than or equal to 6"

    Scenario Outline: Cannot create classroom when the group is <group>
        When I create a classroom for course 1 group "<group>" at school "Gloria Fuertes"
        Then I receive an error stating that "group" field is incorrect because "must match \"^[A-Z]$\""

        Examples:
            | group |
            | a     |
            | 1     |
            | AB    |

    Scenario: Cannot create a duplicated classroom
        Given a classroom for course 1 group "A" exists at school "Gloria Fuertes"
        When I create a classroom for course 1 group "A" at school "Gloria Fuertes"
        Then I receive an error because the classroom already exists
