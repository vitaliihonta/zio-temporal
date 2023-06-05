const sidebars = {
  "docs": [
    {
      type: "category",
      label: "Getting started",
      items: [
        "core/overview",
        "core/workflows",
        "core/activities"
      ]
    },
    {
      type: "category",
      label: "Workflows",
      items: [
        "workflows/state",
        "workflows/queries",
        "workflows/signals",
        "workflows/child-workflows",
        "workflows/continue-as-new",
        "workflows/timers",
        "workflows/external-workflows",
        "workflows/advanced"
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
      type: "doc",
      id: "FAQ",
      label: "FAQ"
    }
  ]
}

module.exports = sidebars;
