from django.urls import path

from .views import calculate_brix, calculate_ledger_chart

urlpatterns = [
    path("calculate", calculate_brix),
    path("ledger/chart", calculate_ledger_chart),
]
