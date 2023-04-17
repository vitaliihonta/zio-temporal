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
        "workflows/child-workflows"
      ]
    },
    {
      type: "category",
      label: "Resilience",
      items: [
        "resilience/retries",
        "resilience/sagas"
      ]
    },
    {
      type: "category",
      label: "Protobuf",
      items: [
        "protobuf/overview",
        "protobuf/usage"
      ]
    },
    {
      type: "category",
      label: "Testing",
      items: [
        "testing/overview"
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
