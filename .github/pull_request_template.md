name: Pull Request
description: Submit a code change
title: "[PR]: "
body:
  - type: markdown
    attributes:
      value: |
        Thank you for contributing! Please complete the following.
  - type: textarea
    id: summary
    attributes:
      label: Summary of Changes
      description: Describe what changes this PR introduces.
      placeholder: "This PR fixes..."
    validations:
      required: true
  - type: textarea
    id: issue-link
    attributes:
      label: Issue Link
      description: Does this PR fix an open issue? If so, provide a link.
      placeholder: "Fixes #123"
  - type: textarea
    id: testing
    attributes:
      label: How did you test this?
      description: Provide details on how you tested this change.
      placeholder: "I ran..."
  - type: checkboxes
    id: checklist
    attributes:
      label: Checklist
      options:
        - label: My code follows the style guide
          required: true
        - label: I have performed a self-review of my code
          required: true
        - label: I have added necessary comments/documentation
        - label: New tests have been added if necessary
        - label: All existing tests pass
