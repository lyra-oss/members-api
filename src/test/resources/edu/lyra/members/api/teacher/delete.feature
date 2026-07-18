Feature: Teacher account deletion

    In order to keep teacher accounts accurate
    As a teacher or Lyra administrator
    I want to delete a teacher's account

    Background:
        Given a school named "Gloria Fuertes" exists
        And the following teachers exist at school "Gloria Fuertes":
            | name  | surname | mail                    |
            | Pablo | Ruiz    | pablo.ruiz@example.com  |
            | José  | García  | jose.garcia@example.com |

    Scenario: An admin can delete an unreferenced teacher
        Given I am authenticated as an admin with "teachers.delete" scope
        When I delete teacher "Pablo" "Ruiz"
        Then I receive a confirmation that the teacher account has been successfully deleted

    Scenario: A teacher can delete their own unreferenced account
        Given I am authenticated as teacher "Pablo Ruiz" with "teachers.delete" scope
        When I delete teacher "Pablo" "Ruiz"
        Then I receive a confirmation that the teacher account has been successfully deleted

    Scenario: A teacher cannot delete another teacher's account
        Given I am authenticated as teacher "José García" with "teachers.delete" scope
        When I delete teacher "Pablo" "Ruiz"
        Then I receive a forbidden error

    Scenario: A parent cannot delete a teacher's account
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
        And I am authenticated as "esteban.cristobal@example.com" with "teachers.delete" scope
        When I delete teacher "Pablo" "Ruiz"
        Then I receive a forbidden error

    Scenario: Cannot delete a teacher that does not exist
        Given I am authenticated as an admin with "teachers.delete" scope
        When I delete a teacher that does not exist
        Then I receive a not found error

    Scenario: An admin cannot delete a teacher who still tutors a classroom
        Given a classroom for course 3 group "A" exists at school "Gloria Fuertes"
        And teacher "Pablo Ruiz" has been set as the classroom's tutor
        And I am authenticated as an admin with "teachers.delete" scope
        When I delete teacher "Pablo" "Ruiz"
        Then I receive a conflict error

    Scenario: A teacher cannot delete their own account while they still tutor a classroom
        Given a classroom for course 3 group "A" exists at school "Gloria Fuertes"
        And teacher "Pablo Ruiz" has been set as the classroom's tutor
        And I am authenticated as teacher "Pablo Ruiz" with "teachers.delete" scope
        When I delete teacher "Pablo" "Ruiz"
        Then I receive a conflict error

    Scenario: A teacher cannot delete their own account while they still teach a classroom without tutoring it
        Given a classroom for course 3 group "A" exists at school "Gloria Fuertes"
        And teacher "Pablo Ruiz" has been added to the classroom
        And I am authenticated as teacher "Pablo Ruiz" with "teachers.delete" scope
        When I delete teacher "Pablo" "Ruiz"
        Then I receive a conflict error
