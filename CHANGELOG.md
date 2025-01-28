## 2024.
* Initial release.

## Jan 2025.
* Moved docs to [https://docs.page/donnc/wce](https://docs.page/donnc/wce)
* Added support for interactive [Call-To-Action](https://developers.facebook.com/docs/whatsapp/cloud-api/messages/interactive-cta-url-messages) message type
```yaml
"MY-CTA-MESSAGE":
  type: cta
  message:
    body: "Next available scan dates are available on our calendar"
    url: "https://my.long.url.com/calendar?event=scan"
    button: "See Dates"
  routes:
    "re:.*": "NEXT-STAGE"
```

## Jan 2025
* Added example custom triggers
* Fixed triggers not working (@)
* Fixed CTA button correct payload creation
