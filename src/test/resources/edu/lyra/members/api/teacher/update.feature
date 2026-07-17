Feature: Teacher account updates

    In order to keep teacher accounts accurate
    As a teacher or Lyra administrator
    I want to update a teacher's account

    Background:
        Given a school named "Gloria Fuertes" exists
        And the following teachers exist at school "Gloria Fuertes":
            | name  | surname | mail                    |
            | Pablo | Ruiz    | pablo.ruiz@example.com  |
            | José  | García  | jose.garcia@example.com |

    Scenario: An admin can update any teacher's account
        Given I am authenticated as an admin with "teachers.update" scope
        When I update teacher "Pablo" "Ruiz"'s surname to "Ruiz Fernández"
        Then I receive a confirmation that the teacher account has been successfully updated

    Scenario: A teacher can update their own account
        Given I am authenticated as teacher "Pablo Ruiz" with "teachers.update" scope
        When I update teacher "Pablo" "Ruiz"'s surname to "Ruiz Fernández"
        Then I receive a confirmation that the teacher account has been successfully updated

    Scenario: A teacher cannot update another teacher's account
        Given I am authenticated as teacher "José García" with "teachers.update" scope
        When I update teacher "Pablo" "Ruiz"'s surname to "Ruiz Fernández"
        Then I receive a forbidden error

    Scenario: A parent cannot update a teacher's account
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
        And I am authenticated as "esteban.cristobal@example.com" with "teachers.update" scope
        When I update teacher "Pablo" "Ruiz"'s surname to "Ruiz Fernández"
        Then I receive a forbidden error

    Scenario: Cannot update a teacher that does not exist
        Given I am authenticated as an admin with "teachers.update" scope
        When I update a teacher that does not exist
        Then I receive a not found error

    Scenario: Cannot update a teacher's account when the surname is left blank
        Given I am authenticated as an admin with "teachers.update" scope
        When I update teacher "Pablo" "Ruiz"'s surname to ""
        Then I receive an error stating that "person.surname" field is incorrect because "must not be blank"
