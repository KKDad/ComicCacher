'use client';

import { useState, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import Link from 'next/link';
import { Loader2 } from 'lucide-react';
import { loginSchema, type LoginFormData } from '@/lib/validations/auth';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PasswordInput } from '@/components/ui/password-input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { Card, CardContent } from '@/components/ui/card';
import { AuthHeader } from '@/components/auth/auth-header';
import { ErrorBanner } from '@/components/auth/error-banner';
import { safeRedirectPath } from '@/lib/safe-redirect';

function LoginForm() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    mode: 'onTouched',
    defaultValues: {
      username: '',
      password: '',
      rememberMe: true,
    },
  });

  const { register, handleSubmit, formState: { errors }, watch } = form;

  const rememberMe = watch('rememberMe');

  const onSubmit = async (data: LoginFormData) => {
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const res = await fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      if (!res.ok) {
        const json = await res.json();
        setErrorMessage(json.error ?? 'Login failed');
        return;
      }

      const redirectTo = safeRedirectPath(searchParams.get('from'));
      router.push(redirectTo);
      router.refresh();
    } catch {
      setErrorMessage('Something went wrong. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card className="bg-surface shadow-lg">
      <AuthHeader
        title="Welcome back"
        description="Sign in to continue to your comic collection"
      />
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          {errorMessage && (
            <ErrorBanner
              message={errorMessage}
              onDismiss={() => setErrorMessage(null)}
            />
          )}

          <div className="space-y-2">
            <Label htmlFor="username">Username or Email</Label>
            <Input
              id="username"
              type="text"
              placeholder="Enter your username or email"
              autoComplete="username"
              disabled={isSubmitting}
              aria-invalid={!!errors.username}
              aria-describedby={errors.username ? 'username-error' : undefined}
              {...register('username')}
            />
            {errors.username && (
              <p id="username-error" className="text-sm text-error">{errors.username.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="password">Password</Label>
              <Link
                href="/forgot-password"
                className="text-sm text-primary hover:text-primary-hover transition-colors"
              >
                Forgot password?
              </Link>
            </div>
            <PasswordInput
              id="password"
              placeholder="Enter your password"
              autoComplete="current-password"
              disabled={isSubmitting}
              aria-invalid={!!errors.password}
              aria-describedby={errors.password ? 'password-error' : undefined}
              {...register('password')}
            />
            {errors.password && (
              <p id="password-error" className="text-sm text-error">{errors.password.message}</p>
            )}
          </div>

          <div className="flex items-center space-x-2">
            <Checkbox
              id="rememberMe"
              disabled={isSubmitting}
              checked={rememberMe}
              onCheckedChange={(checked) => {
                form.setValue('rememberMe', checked === true);
              }}
            />
            <Label
              htmlFor="rememberMe"
              className="text-sm font-normal cursor-pointer"
            >
              Keep me signed in
            </Label>
          </div>

          <Button
            type="submit"
            className="w-full"
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Signing in...
              </>
            ) : (
              'Sign in'
            )}
          </Button>

          <div className="text-center text-sm text-ink-subtle">
            Don&apos;t have an account?{' '}
            <Link
              href="/register"
              className="text-primary hover:text-primary-hover font-medium transition-colors"
            >
              Create one
            </Link>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <Card className="bg-surface shadow-lg">
          <CardContent className="p-8">
            <div className="text-center text-ink-subtle">Loading...</div>
          </CardContent>
        </Card>
      }
    >
      <LoginForm />
    </Suspense>
  );
}
