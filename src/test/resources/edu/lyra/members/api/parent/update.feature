Feature: Parent account updates

    In order to keep parent accounts accurate
    As a parent or Lyra administrator
    I want to update a parent's account

    Background:
        Given the following parents exist:
            | name    | surname   | mail                          |
            | Esteban | Cristóbal | esteban.cristobal@example.com |
            | Marta   | Ibáñez    | marta.ibanez@example.com      |

    Scenario: An admin can update any parent's account
        Given I am authenticated as an admin with "parents.update" scope
        When I update parent "Esteban" "Cristóbal"'s surname to "Cristóbal Ruiz"
        Then I receive a confirmation that the account has been successfully updated

    Scenario: A parent can update their own account
        Given I am authenticated as "esteban.cristobal@example.com" with "parents.update" scope
        When I update parent "Esteban" "Cristóbal"'s surname to "Cristóbal Ruiz"
        Then I receive a confirmation that the account has been successfully updated

    Scenario: A parent cannot update another parent's account
        Given I am authenticated as "marta.ibanez@example.com" with "parents.update" scope
        When I update parent "Esteban" "Cristóbal"'s surname to "Cristóbal Ruiz"
        Then I receive a forbidden error

    Scenario: A teacher cannot update a parent's account
        Given a school named "Gloria Fuertes" exists
        And a teacher named "Pablo" "Ruiz" exists at school "Gloria Fuertes" with e-mail "pablo.ruiz@example.com"
        And I am authenticated as teacher "Pablo Ruiz" with "parents.update" scope
        When I update parent "Esteban" "Cristóbal"'s surname to "Cristóbal Ruiz"
        Then I receive a forbidden error

    Scenario: Cannot update a parent that does not exist
        Given I am authenticated as an admin with "parents.update" scope
        When I update a parent that does not exist
        Then I receive a not found error

    Scenario: Cannot update a parent's account when the surname is left blank
        Given I am authenticated as an admin with "parents.update" scope
        When I update parent "Esteban" "Cristóbal"'s surname to ""
        Then I receive an error stating that "person.surname" field is incorrect because "must not be blank"
