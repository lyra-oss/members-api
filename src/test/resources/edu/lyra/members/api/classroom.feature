Feature: Classroom teaching staff

    In order to manage who teaches each classroom
    As Lyra
    I want to assign teachers and a tutor to a classroom

    Background:
        Given I am authenticated with "schools.create" scope
        And a school named "Gloria Fuertes" exists
        And a classroom for course 3 group "A" exists at school "Gloria Fuertes"
        And a teacher named "Marta" "Ibáñez" exists at school "Gloria Fuertes" with e-mail "marta.ibanez@example.com"

    Scenario: Add a teacher to a classroom
        When I add teacher "Marta Ibáñez" to the classroom
        Then I receive a confirmation that the teacher has been successfully added to the classroom

    Scenario: Get a classroom's teachers
        Given teacher "Marta Ibáñez" has been added to the classroom
        When I request the classroom's teachers
        Then the classroom's teachers include "Marta Ibáñez"

    Scenario: Set a classroom's tutor
        When I set teacher "Marta Ibáñez" as the classroom's tutor
        Then I receive a confirmation that the tutor has been successfully set

    Scenario: Get a classroom's tutor
        Given teacher "Marta Ibáñez" has been set as the classroom's tutor
        When I request the classroom's tutor
        Then the classroom's tutor is "Marta Ibáñez"

    Scenario: Cannot get a classroom's tutor when none has been assigned
        When I request the classroom's tutor
        Then I receive a not found error

    Scenario: Cannot add a teacher from a different school to a classroom
        Given a school named "Montessori Norte" exists
        And a teacher named "Pablo" "Ruiz" exists at school "Montessori Norte" with e-mail "pablo.ruiz@example.com"
        When I add teacher "Pablo Ruiz" to the classroom
        Then I receive an error because the teacher does not belong to the classroom's school

    Scenario: Cannot set a tutor from a different school
        Given a school named "Montessori Norte" exists
        And a teacher named "Pablo" "Ruiz" exists at school "Montessori Norte" with e-mail "pablo.ruiz@example.com"
        When I set teacher "Pablo Ruiz" as the classroom's tutor
        Then I receive an error because the teacher does not belong to the classroom's school

    Scenario: Cannot add a teacher to a classroom that does not exist
        When I add teacher "Marta Ibáñez" to a classroom that does not exist
        Then I receive a not found error

    Scenario: Cannot add a teacher that does not exist
        When I add a teacher that does not exist to the classroom
        Then I receive a bad request error

    Scenario: Cannot add a teacher without specifying which one
        When I add a teacher to the classroom without specifying which one
        Then I receive a bad request error
