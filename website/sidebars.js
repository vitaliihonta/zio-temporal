const sidebars = {
  "docs": [
    {
      type: "category",
      label: "Getting started",
      items: [
        "core/overview",
        "core/workflows",
        "core/activities",
        "core/configuration",
        "core/workers"
      ]
    },
    {
      type: "category",
      label: "Workflows",
      items: [
        "workflows/queries",
        "workflows/signals",
        "workflows/child-workflows",
        "workflows/state",
        "workflows/continue-as-new",
        "workflows/timers",
        "workflows/external-workflows",
        "workflows/schedules",
        "workflows/versioning"
      ]
    },
    {
      type: "category",
      label: "Resilience",
      items: [
        "resilience/retries",
        "resilience/sagas",
        "resilience/heartbeats"
      ]
    },
    {
      type: "category",
      label: "Serialization",
      items: [
        "serialization/overview",
        "serialization/jackson",
        "serialization/protobuf"
      ]
    },
    {
      type: "category",
      label: "Testing",
      items: [
        "testing/overview",
        "testing/testing-activities",
        "testing/testing-workflows"
      ]
    },
    {
      "type": "doc",
      "id": "observability",
      "label": "Observability"
    },
    {
      type: "category",
      label: "Advanced",
      items: [
        "advanced/overview",
        "advanced/workflow-polymorphism",
        "advanced/generic-workflows"
      ]
    },
    {
      type: "category",
      label: "Tutorial",
      items: [
        {
          type: "doc",
          id: "tutorial/tutorial-intro",
          label: "Background Prerequisites"
        },
        {
          type: "doc",
          id: "tutorial/environment-setup",
          label: "Setting Up the Tutorial Environment"
        },
        {
          type: "doc",
          id: "tutorial/workflow-definition",
          label: "Defining a Workflow"
        },
        {
          type: "doc",
          id: "tutorial/worker-creation",
          label: "Creating a Worker"
        },
        {
          type: "doc",
          id: "tutorial/workflow-execution",
          label: "Executing a Workflow"
        },
        {
          type: "doc",
          id: "tutorial/json-io",
          label: "Using a Complex Workflow Parameter"
        },
        {
          type: "doc",
          id: "tutorial/activities",
          label: "Using a Temporal Activity"
        },
        {
          type: "doc",
          id: "tutorial/next-steps",
          label: "Next Steps for Learning"
        },
      ]
    },
    {
      type: "doc",
      id: "FAQ",
      label: "FAQ"
    },
    {
      type: "doc",
      id: "contribution",
      label: "Contribution"
    }
  ]
}

module.exports = sidebars;
