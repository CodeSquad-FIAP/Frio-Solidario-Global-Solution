using FrioSolidario.Application.DTOs;

namespace FrioSolidario.Application.Services;

/// <summary>A porta de entrada do sistema: sem token nenhuma tela do app funciona.</summary>
public interface IAuthService
{
    Task<LoginResponse> LoginAsync(LoginRequest request, CancellationToken cancellationToken = default);
}
