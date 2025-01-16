## 2024.
* Initial release.

## Jan 2025.
* Moved docs to [https://docs.page/donnc/wce](https://docs.page/donnc/wce)
* Added support for interactive [Call-To-Action](https://developers.facebook.com/docs/whatsapp/cloud-api/messages/interactive-cta-url-messages) message type
```yaml
"MY-CTA-MESSAGE":
  type: cta
  message:
    title: "JawceMedic"
    body: "Next available scan dates are available on our calendar"
    button: "See Dates"
  routes:
    "re:.*": "NEXT-STAGE"
```
