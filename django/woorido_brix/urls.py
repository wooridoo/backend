from django.urls import include, path

urlpatterns = [
    path("internal/brix/", include("brix.urls")),
]
